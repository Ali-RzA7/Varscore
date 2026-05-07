package com.example.var.util;

import androidx.annotation.NonNull;

import com.example.var.data.model.UserModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FirebaseManager - Firebase Authentication ve Firestore işlemlerini yöneten merkezi sınıf.
 *
 * Uygulamadaki tüm Firebase işlemleri bu sınıf üzerinden yapılır:
 * - Kullanıcı kimlik doğrulama durumu kontrolü
 * - Firestore'a kullanıcı profili okuma/yazma
 * - Favori takım ve lig ekleme/çıkarma/listeleme
 *
 * Firestore Veri Yapısı:
 *   users/{userId}/
 *     displayName: String
 *     email: String
 *     photoUrl: String
 *     favoriteTeams: List<String>   → takım ID'leri
 *     favoriteLeagues: List<String> → lig ID'leri
 *
 * Favori Ligler Mantığı:
 *   Kullanıcı bir ligi favoriye eklediğinde, o ligin TÜM takımlarının
 *   maçları için bildirim alabilir. Tek tek takım eklemeye gerek kalmaz.
 */
public class FirebaseManager {

    /** Firebase Authentication instance */
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();

    /** Firebase Firestore instance */
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Firestore'daki kullanıcı koleksiyonu adı */
    private static final String USERS_COLLECTION = "users";

    /** Firestore'daki favori takımlar alanı */
    private static final String FIELD_FAVORITE_TEAMS = "favoriteTeams";

    /** Firestore'daki favori ligler alanı */
    private static final String FIELD_FAVORITE_LEAGUES = "favoriteLeagues";

    // ===== Auth İşlemleri =====

    /**
     * Giriş yapmış kullanıcının Firebase nesnesini döndürür.
     *
     * @return Giriş yapılmışsa FirebaseUser, değilse null
     */
    public static FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /**
     * Kullanıcının giriş yapmış olup olmadığını kontrol eder.
     *
     * @return true ise giriş yapılmış
     */
    public static boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    /**
     * Kullanıcıyı oturumdan çıkarır.
     * AccountFragment'taki "Çıkış Yap" butonundan çağrılır.
     */
    public static void signOut() {
        auth.signOut();
    }

    // ===== Profil İşlemleri =====

    /**
     * Firestore'a yeni kullanıcı profili kaydeder.
     * Kayıt veya Google ile giriş sonrası çağrılır.
     * Eğer profil zaten varsa (Google ile tekrar giriş) üzerine yazmaz.
     *
     * @param user      Kaydedilecek kullanıcı modeli
     * @param onSuccess Başarı callback'i
     * @param onFailure Hata callback'i
     */
    public static void saveUserProfile(UserModel user,
            OnSuccessListener<Void> onSuccess,
            OnFailureListener onFailure) {

        DocumentReference docRef = db.collection(USERS_COLLECTION).document(user.getUserId());

        // Önce doküman var mı kontrol et (var ise favori listelerini korumak için)
        docRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                // İlk kez kaydediliyor, tüm alanları yaz
                Map<String, Object> data = new HashMap<>();
                data.put("displayName", user.getDisplayName());
                data.put("email", user.getEmail());
                data.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl() : "");
                data.put(FIELD_FAVORITE_TEAMS, user.getFavoriteTeams());
                data.put(FIELD_FAVORITE_LEAGUES, user.getFavoriteLeagues());

                docRef.set(data)
                        .addOnSuccessListener(onSuccess)
                        .addOnFailureListener(onFailure);
            } else {
                // Profil zaten var, sadece temel bilgileri güncelle
                Map<String, Object> updates = new HashMap<>();
                updates.put("displayName", user.getDisplayName());
                updates.put("email", user.getEmail());
                if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                    updates.put("photoUrl", user.getPhotoUrl());
                }
                docRef.update(updates)
                        .addOnSuccessListener(onSuccess)
                        .addOnFailureListener(onFailure);
            }
        }).addOnFailureListener(onFailure);
    }

    /**
     * Firestore'dan kullanıcı profilini okur.
     * ProfileFragment açıldığında çağrılır.
     *
     * @param userId    Okunacak kullanıcının ID'si (FirebaseUser.getUid())
     * @param onSuccess Başarı callback'i - UserModel döner
     * @param onFailure Hata callback'i
     */
    public static void getUserProfile(String userId,
            OnSuccessListener<UserModel> onSuccess,
            OnFailureListener onFailure) {

        db.collection(USERS_COLLECTION).document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // Firestore'dan gelen Map'i UserModel'e dönüştür
                        UserModel user = new UserModel();
                        user.setUserId(userId);
                        user.setDisplayName(snapshot.getString("displayName"));
                        user.setEmail(snapshot.getString("email"));
                        user.setPhotoUrl(snapshot.getString("photoUrl"));

                        // Favori listelerini al (null kontrolü ile)
                        List<String> teams = (List<String>) snapshot.get(FIELD_FAVORITE_TEAMS);
                        List<String> leagues = (List<String>) snapshot.get(FIELD_FAVORITE_LEAGUES);
                        if (teams != null) user.setFavoriteTeams(teams);
                        if (leagues != null) user.setFavoriteLeagues(leagues);

                        onSuccess.onSuccess(user);
                    } else {
                        // Profil yok (ilk giriş sonrası oluşturulmamış)
                        onFailure.onFailure(new Exception("Profil bulunamadı"));
                    }
                })
                .addOnFailureListener(onFailure);
    }

    // ===== Favori Takım İşlemleri =====

    /**
     * Kullanıcının favori takımlar listesine yeni bir takım ekler.
     * Takım zaten listedeyse işlem yapılmaz (Firestore FieldValue.arrayUnion ile).
     *
     * @param teamId    Eklenecek takımın iSportsAPI ID'si
     * @param onSuccess Başarı callback'i
     * @param onFailure Hata callback'i
     */
    public static void addFavoriteTeam(String teamId,
            OnSuccessListener<Void> onSuccess,
            OnFailureListener onFailure) {

        FirebaseUser user = getCurrentUser();
        if (user == null) {
            onFailure.onFailure(new Exception("Kullanıcı giriş yapmamış"));
            return;
        }

        db.collection(USERS_COLLECTION).document(user.getUid())
                .update(FIELD_FAVORITE_TEAMS,
                        com.google.firebase.firestore.FieldValue.arrayUnion(teamId))
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Kullanıcının favori takımlar listesinden bir takımı çıkarır.
     *
     * @param teamId    Çıkarılacak takımın ID'si
     * @param onSuccess Başarı callback'i
     * @param onFailure Hata callback'i
     */
    public static void removeFavoriteTeam(String teamId,
            OnSuccessListener<Void> onSuccess,
            OnFailureListener onFailure) {

        FirebaseUser user = getCurrentUser();
        if (user == null) return;

        db.collection(USERS_COLLECTION).document(user.getUid())
                .update(FIELD_FAVORITE_TEAMS,
                        com.google.firebase.firestore.FieldValue.arrayRemove(teamId))
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // ===== Favori Lig İşlemleri =====

    /**
     * Kullanıcının favori ligler listesine yeni bir lig ekler.
     *
     * Önemli: Lig favoriye eklenince kullanıcı o ligin TÜM takımlarının
     * maçlarını takip etmeye başlar. Bu, tek tek takım eklemeye alternatiftir.
     * Örneğin "Süper Lig" favoriye eklenince 18 takımın tüm maçları takip edilir.
     *
     * @param leagueId  Eklenecek ligin iSportsAPI ID'si
     * @param onSuccess Başarı callback'i
     * @param onFailure Hata callback'i
     */
    public static void addFavoriteLeague(String leagueId,
            OnSuccessListener<Void> onSuccess,
            OnFailureListener onFailure) {

        FirebaseUser user = getCurrentUser();
        if (user == null) {
            onFailure.onFailure(new Exception("Kullanıcı giriş yapmamış"));
            return;
        }

        db.collection(USERS_COLLECTION).document(user.getUid())
                .update(FIELD_FAVORITE_LEAGUES,
                        com.google.firebase.firestore.FieldValue.arrayUnion(leagueId))
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Kullanıcının favori ligler listesinden bir ligi çıkarır.
     *
     * @param leagueId  Çıkarılacak ligin ID'si
     * @param onSuccess Başarı callback'i
     * @param onFailure Hata callback'i
     */
    public static void removeFavoriteLeague(String leagueId,
            OnSuccessListener<Void> onSuccess,
            OnFailureListener onFailure) {

        FirebaseUser user = getCurrentUser();
        if (user == null) return;

        db.collection(USERS_COLLECTION).document(user.getUid())
                .update(FIELD_FAVORITE_LEAGUES,
                        com.google.firebase.firestore.FieldValue.arrayRemove(leagueId))
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Verilen takımın kullanıcının favorilerinde olup olmadığını kontrol eder.
     * Doğrudan takım favorileri VE lig favorileri üzerinden kontrol yapar.
     * Not: Lig bazlı kontrol için önce takımın ligini bilmek gerekir.
     *
     * @param teamId    Kontrol edilecek takım ID'si
     * @param leagueId  Takımın bağlı olduğu lig ID'si
     * @param onResult  Boolean callback - true ise favori
     */
    public static void isTeamOrLeagueFavorite(String teamId, String leagueId,
            OnSuccessListener<Boolean> onResult) {

        FirebaseUser user = getCurrentUser();
        if (user == null) {
            onResult.onSuccess(false);
            return;
        }

        db.collection(USERS_COLLECTION).document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        onResult.onSuccess(false);
                        return;
                    }
                    List<String> teams = (List<String>) snapshot.get(FIELD_FAVORITE_TEAMS);
                    List<String> leagues = (List<String>) snapshot.get(FIELD_FAVORITE_LEAGUES);

                    boolean teamFav = teams != null && teams.contains(teamId);
                    boolean leagueFav = leagues != null && leagues.contains(leagueId);

                    onResult.onSuccess(teamFav || leagueFav);
                });
    }
}
