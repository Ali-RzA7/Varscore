package com.example.var.data.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserModel - Firebase Firestore'da saklanan kullanıcı profilini temsil eder.
 *
 * Firestore koleksiyon yapısı:
 *   users/{userId}/
 *     - displayName: String
 *     - email: String
 *     - photoUrl: String
 *     - favoriteTeams: List<String>   (teamId listesi)
 *     - favoriteLeagues: List<String> (leagueId listesi)
 *
 * Favori Mantığı:
 * - favoriteTeams: Belirli takımları takip eder, o takımların maçları için bildirim alır
 * - favoriteLeagues: Bir ligin TÜM takımlarını takip eder; ayrı ayrı eklemeye gerek kalmaz
 *   Örnek: "Süper Lig" favoriye eklenince ligin 18 takımının tüm maçları takip edilir
 *
 * Kullanım:
 * - FirebaseManager.saveUserProfile() ile Firestore'a yazılır
 * - FirebaseManager.getUserProfile() ile Firestore'dan okunur
 * - ProfileFragment'ta görüntülenir ve düzenlenir
 */
public class UserModel {

    /** Firebase Authentication UID'si - Firestore doküman ID olarak kullanılır */
    private String userId;

    /** Kullanıcının görünen adı (displayName) */
    private String displayName;

    /** Kullanıcının e-posta adresi */
    private String email;

    /** Firebase Storage'daki profil fotoğrafı URL'si */
    private String photoUrl;

    /**
     * Favori takımların ID listesi.
     * Her eleman iSportsAPI'deki homeId/awayId ile eşleşir.
     */
    private List<String> favoriteTeams;

    /**
     * Favori liglerin ID listesi.
     * Her eleman iSportsAPI'deki leagueId ile eşleşir.
     * Bu listede yer alan bir ligin TÜM maçları takip edilir.
     */
    private List<String> favoriteLeagues;

    /** Favori takımların adları: teamId → teamName */
    private Map<String, String> favoriteTeamNames;

    /** Favori takımların ligleri: teamId → leagueId */
    private Map<String, String> favoriteTeamLeagues;

    /** Favori liglerin adları: leagueId → leagueName */
    private Map<String, String> favoriteLeagueNames;

    /**
     * Firestore deserialization için boş yapıcı metot.
     * Firestore, nesneyi oluştururken bu yapıcıyı kullanır.
     */
    public UserModel() {
        this.favoriteTeams = new ArrayList<>();
        this.favoriteLeagues = new ArrayList<>();
        this.favoriteTeamNames = new HashMap<>();
        this.favoriteTeamLeagues = new HashMap<>();
        this.favoriteLeagueNames = new HashMap<>();
    }

    /**
     * Yeni kullanıcı profili oluşturan yapıcı metot.
     *
     * @param userId      Firebase Auth UID
     * @param displayName Kullanıcının görünen adı
     * @param email       E-posta adresi
     */
    public UserModel(String userId, String displayName, String email) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.photoUrl = "";
        this.favoriteTeams = new ArrayList<>();
        this.favoriteLeagues = new ArrayList<>();
        this.favoriteTeamNames = new HashMap<>();
        this.favoriteTeamLeagues = new HashMap<>();
        this.favoriteLeagueNames = new HashMap<>();
    }

    // ===== Getter ve Setter Metodları =====

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public List<String> getFavoriteTeams() {
        return favoriteTeams != null ? favoriteTeams : new ArrayList<>();
    }
    public void setFavoriteTeams(List<String> favoriteTeams) { this.favoriteTeams = favoriteTeams; }

    public List<String> getFavoriteLeagues() {
        return favoriteLeagues != null ? favoriteLeagues : new ArrayList<>();
    }
    public void setFavoriteLeagues(List<String> favoriteLeagues) { this.favoriteLeagues = favoriteLeagues; }

    public Map<String, String> getFavoriteTeamNames() {
        return favoriteTeamNames != null ? favoriteTeamNames : new HashMap<>();
    }
    public void setFavoriteTeamNames(Map<String, String> favoriteTeamNames) { this.favoriteTeamNames = favoriteTeamNames; }

    public Map<String, String> getFavoriteTeamLeagues() {
        return favoriteTeamLeagues != null ? favoriteTeamLeagues : new HashMap<>();
    }
    public void setFavoriteTeamLeagues(Map<String, String> favoriteTeamLeagues) { this.favoriteTeamLeagues = favoriteTeamLeagues; }

    public Map<String, String> getFavoriteLeagueNames() {
        return favoriteLeagueNames != null ? favoriteLeagueNames : new HashMap<>();
    }
    public void setFavoriteLeagueNames(Map<String, String> favoriteLeagueNames) { this.favoriteLeagueNames = favoriteLeagueNames; }

    // ===== Yardımcı Metodlar =====

    /**
     * Verilen takımın favorilerde olup olmadığını kontrol eder.
     *
     * @param teamId Kontrol edilecek takım ID'si
     * @return true ise takım favorilerde mevcut
     */
    public boolean isTeamFavorite(String teamId) {
        return favoriteTeams != null && favoriteTeams.contains(teamId);
    }

    /**
     * Verilen ligin favorilerde olup olmadığını kontrol eder.
     *
     * @param leagueId Kontrol edilecek lig ID'si
     * @return true ise lig favorilerde mevcut
     */
    public boolean isLeagueFavorite(String leagueId) {
        return favoriteLeagues != null && favoriteLeagues.contains(leagueId);
    }
}
