# VarScore Proje Detayları

## Proje Özeti
Android futbol uygulaması. Canlı skor, lig istatistikleri ve Groq API ile AI maç tahmini sunar.

---

## Mimari

**Dil:** Java | **Min SDK:** 33 | **Target SDK:** 36  
**Pattern:** Repository Pattern (ViewModel yok, Fragment → Repository direkt)  
**UI:** ViewBinding + Material Design + AppCompat  
**Async:** Retrofit Callback  

### Paket Yapısı
```
com.example.var/
├── data/model/       — MatchModel, UserModel, LeagueModel, EventModel, StatModel...
├── data/remote/      — FootballApiService, RetrofitClient, GroqApiService
├── data/repository/  — MatchRepository (tüm API çağrıları)
├── ui/fragment/      — HomeFragment, MatchDetailFragment, ProfileFragment, Login/Register...
├── ui/adapter/       — MatchAdapter, DatePickerAdapter, StandingsTableAdapter...
├── ui/dialog/        — SearchDialog, SettingsDialog, AIPredictionDialog, CalendarDialog
└── util/             — FirebaseManager, MatchCache, DateUtils, NotificationHelper...
```

---

## Önemli Sınıflar

### HomeFragment
Ana ekran. Canlı/Tüm Maçlar toggle, tarih seçici, lig bazlı maç listesi.
- `loadMatchesForDate(calendar)` — tarihe göre maç yükler (cache + API)
- `loadLiveMatches()` — canlı maçları yükler, 15 sn polling
- `loadUserFavorites()` — giriş yapmış kullanıcının favorilerini Firebase'den çeker
- `sortMatchesByFavorites(matches)` — favori lig/takım gruplarını listenin başına taşır
- `onResume()` — favorileri yeniler (kullanıcı profil ekranından dönünce)

### MatchAdapter
RecyclerView adapter. `TYPE_LEAGUE_HEADER` + `TYPE_MATCH` ViewHolder'ı var.
- `setMatches(list)` — leagueName'e göre gruplar, `isFavorite` flag'i hesaplar
- `setFavorites(teamIds, leagueIds)` — favori setleri günceller
- `setTeamLogoMap(map)` — standings'ten gelen takım logoları
- LeagueHeader: `leagueId`, `leagueName`, `leagueColor`, `leagueType`, `isFavorite`

### FirebaseManager
Tüm Firebase işlemleri buradan. Static metodlar.
- `isLoggedIn()`, `getCurrentUser()`, `signOut()`
- `getUserProfile(userId, onSuccess, onFailure)`
- `addFavoriteTeam/League(...)`, `removeFavoriteTeam/League(...)`
- `isTeamOrLeagueFavorite(teamId, leagueId, callback)`

### UserModel
Firestore kullanıcı profili.
- `favoriteTeams: List<String>` — teamId listesi
- `favoriteLeagues: List<String>` — leagueId listesi
- `favoriteTeamNames/LeagueNames/TeamLeagues: Map<String, String>`
- `isTeamFavorite(id)`, `isLeagueFavorite(id)`

### MatchCache
SharedPreferences tabanlı. TTL: 60 saniye. Key: `matches_` + tarih.

---

## Favori Özelliği (Son Güncelleme)

### Ne Yapıyor
- Sadece giriş yapmış kullanıcılar favori ekleyebilir
- Favori lig veya takımın maçı varsa o lig grubu en üstte gösterilir
- Hem "Tüm Maçlar" hem "Canlı Maçlar" modunda çalışır
- Birden fazla favori desteklenir

### Sıralama Mantığı
Lig grubu favori sayılır eğer:
1. Ligin leagueId'si `favoriteLeagues`'da ise, **VEYA**
2. Gruptaki herhangi maçın homeId veya awayId `favoriteTeams`'da ise

### Görsel Göstergeler
- **Lig başlığında ★** (sarı yıldız) — lig favoriyse
- **Maç satırında ★** (sarı yıldız) — ev sahibi veya deplasman takımı favoriyse

### Değiştirilen Dosyalar
| Dosya | Değişiklik |
|-------|-----------|
| `HomeFragment.java` | `loadUserFavorites()`, `sortMatchesByFavorites()`, `isLeagueGroupFavorite()` metodları eklendi; `loadMatchesForDate/loadLiveMatches` güncellendi; `onResume` güncellendi |
| `MatchAdapter.java` | `setFavorites()` metodu, `favoriteTeamIds/LeagueIds` alanları, `LeagueHeader.isFavorite+leagueId`, `tvFavoriteStar` ve `tvTeamFavoriteStar` ViewHolder alanları eklendi |
| `item_league_header.xml` | `tvFavoriteStar` TextView eklendi |
| `item_match.xml` | `tvTeamFavoriteStar` TextView eklendi |

---

## Bildirim Sistemi
WorkManager (`MatchMonitorWorker`) + BroadcastReceiver (`MatchReminderReceiver`) + `NotificationHelper`.
Bildirim kanalları: Canlı Maç Bildirimleri + Maç Hatırlatmaları.

---

## Veri Akışı (Ana Sayfa)
```
onViewCreated()
  → loadUserFavorites() [async Firebase]
  → loadMatchesForDate(bugün) [async]
      → MatchCache.load() → varsa sortMatchesByFavorites → adapter
      → API → MatchCache.save() → sortMatchesByFavorites → adapter
onResume()
  → loadUserFavorites() [favorileri yenile]
```
