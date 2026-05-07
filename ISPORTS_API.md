# iSports API — Football Endpoint Referansı

Base URL: `http://api.isportsapi.com/sport/football/`
Auth: Tüm isteklere `?api_key=YOUR_KEY` eklenmeli.

---

## 1. Schedule & Results (Basic)
**Path:** `GET /sport/football/schedule/basic`
**Limit:** 60 sn/call | Tavsiye: 12 saatte 1

### Parametreler
| Parametre | Zorunlu | Açıklama |
|---|---|---|
| date | Hayır | yyyy-MM-dd — geçmiş 1 ay sorgulanabilir |
| leagueId | Hayır | Lig ID'si |
| season | Hayır | leagueId ile kullanılır, örn: 2018-2019 |
| matchId | Hayır | Virgülle ayrılmış, max 100 |

> **NOT:** date, leagueId, matchId aynı anda kullanılamaz. En az biri zorunlu.
> **teamId parametresi YOKTUR** — takım maçları için bu endpoint kullanılamaz.

### Yanıt Alanları
`matchId`, `leagueId`, `leagueType`(1=Lig,2=Kupa), `leagueName`, `leagueShortName`, `leagueColor`, `matchTime`(unix), `status`(0=Başlamadı,1=İlkYarı,2=Devre,3=İkinciYarı,4=UzatmaVakti,5=Penaltı,-1=Bitti,-10=İptal,-11=TBD,-12=Sonlandırıldı,-13=Kesintili,-14=Ertelendi), `homeId`, `homeName`, `awayId`, `awayName`, `homeScore`, `awayScore`, `homeHalfScore`, `awayHalfScore`, `explain`, `extraExplain`(kickOff,minute,homeScore,awayScore,extraTimeStatus,extraHomeScore,extraAwayScore,penHomeScore,penAwayScore,twoRoundsHomeScore,twoRoundsAwayScore,winner), `neutral`(boolean)

---

## 2. Schedule & Results (Detaylı)
**Path:** `GET /sport/football/schedule`
**Limit:** 60 sn/call | Tavsiye: 12 saatte 1
**Plan:** Live Data

### Parametreler
| Parametre | Zorunlu | Açıklama |
|---|---|---|
| date | Hayır | yyyy-mm-dd |
| leagueId | Hayır | subLeagueId, stageId ile birlikte kullanılabilir |
| season | Hayır | leagueId ile kullanılır |
| subLeagueId | Hayır | leagueId ile kullanılır |
| stageId | Hayır | leagueId ile kullanılır |
| matchId | Hayır | Virgülle ayrılmış, max 100 |

### Ek Yanıt Alanları (Basic'e ek olarak)
`homeRed`, `awayRed`, `homeYellow`, `awayYellow`, `homeCorner`, `awayCorner`, `homeRank`, `awayRank`, `season`, `stageId`, `round`, `group`, `location`, `weather`, `temperature`, `hasLineup`(boolean), `injuryTime`, `var`, `updateTime`, `halfStartTime`

---

## 3. Livescores (Günün Canlı Maçları)
**Path:** `GET /sport/football/livescores`
**Limit:** 10 sn/call | Tavsiye: 1 dakikada 1
**Plan:** Live Data

### Parametreler
Sadece `api_key`.

### Yanıt Alanları
Basic + Detaylı alanların tamamı + `halfStartTime`, `homeRed`, `awayRed`, `homeYellow`, `awayYellow`, `homeCorner`, `awayCorner`, `homeRank`, `awayRank`, `season`, `round`, `group`, `location`, `weather`, `temperature`, `hasLineup`, `injuryTime`, `var`, `updateTime`

---

## 4. Livescores Changes (Değişiklikler)
**Path:** `GET /sport/football/livescores/changes`
**Limit:** 10 sn/call | Tavsiye: 15 saniyede 1
**Plan:** Live Data

Son 20 saniyedeki değişen maçları döner. Değişiklik yoksa boş liste.

---

## 5. League & Cup Profile (Basic)
**Path:** `GET /sport/football/league/basic`
**Limit:** 1800 sn/call | Tavsiye: Günde 1

### Parametreler
| Parametre | Zorunlu | Açıklama |
|---|---|---|
| leagueId | Hayır | Belirtilmezse tüm ligler döner |

### Yanıt Alanları
`leagueId`, `name`, `shortName`, `type`(1=Lig,2=Kupa), `subLeagueName`

---

## 6. List of Countries
**Path:** `GET /sport/football/country`
**Limit:** 1800 sn/call

### Yanıt Alanları
`countryId`, `country`

---

## 7. League Standing (Lig Puan Durumu) ⚠️
**Path:** `GET /sport/football/standing/league`
**Limit:** 3 sn/call | Tavsiye: Günde 1
**Plan:** Stats

### Parametreler
| Parametre | Zorunlu | Açıklama |
|---|---|---|
| leagueId | Evet | |
| subLeagueId | Hayır | leagueId ile birlikte kullanılır |

### Yanıt Yapısı (KRİTİK)
```
data: {
  leagueInfo: {
    leagueId, name, shortName, logo, color,
    totalRound, currentRound, currentSeason,
    subLeagueInfos: [ { subLeagueId, name, totalRound, currentRound, hasScore, hasTwoLegs, currentSubLeague } ],
    teamInfos: [ { teamId, name, logo, area } ]  ← TAKIM ADLARI BURADA
  },
  totalStandings: [ ... ],   ← standings item'larında name YOKTUR, teamId ile teamInfos'tan alınır
  halfStandings: [ ... ],
  homeStandings: [ ... ],
  awayStandings: [ ... ],
  homeHalfStandings: [ ... ],
  awayHalfStandings: [ ... ]
}
```

### Standing Item Alanları
`rank`, `teamId`, `winRate`, `drawRate`, `loseRate`, `winAverage`, `loseAverage`, `totalCount`(oynanan), `winCount`(galibiyet), `drawCount`(beraberlik), `loseCount`(mağlubiyet), `getScore`(atılan gol), `loseScore`(yenilen gol), `goalDifference`, `integral`(puan), `deduction`, `deductionExplain`, `red`, `totalAddScore`, `color`(yükselme/küme düşme rengi), `recentFirstResult~recentSixthResult`(0=G,1=B,2=M,3=boş)

### leagueColorInfos Alanları
`color`, `leagueName`, `beginRank`, `endRank`

---

## 8. Cup Standing (Kupa Puan Durumu)
**Path:** `GET /sport/football/standing/cup`
**Limit:** 3 sn/call | Tavsiye: Günde 1
**Plan:** Stats

### Parametreler
| Parametre | Zorunlu |
|---|---|
| leagueId | Evet |

### Yanıt Yapısı
`leagueId`, `season`, `roundScoreItems`: [ { `roundName`, `groupScoreItems`: [ { `groupName`, `scoreItems`: [ { `rank`, `teamId`, `teamName`, `color`, `totalCount`, `winCount`, `drawCount`, `loseCount`, `getScore`, `loseScore`, `goalDifference`, `integral`, `deduction`, `deductionExplain` } ] } ] } ]

---

## 9. League Standing — Get Subleague
**Path:** `GET /sport/football/standing/league/getsub`
**Plan:** Stats

### Parametreler
| Parametre | Zorunlu | Açıklama |
|---|---|---|
| leagueId | Evet | Alt lig listesini getirir |

### Yanıt Alanları
`subLeagueId`, `currentSubLeague`(boolean)

---

## 10. Top Scorer (Gol Krallığı)
**Path:** `GET /sport/football/topscorer`
**Limit:** 10 sn/call | Tavsiye: Günde 1
**Plan:** Stats

### Parametreler
| Parametre | Zorunlu | Açıklama |
|---|---|---|
| leagueId | Evet | |
| season | Hayır | Belirli sezon için, örn: 2019 veya 2018-2019 |

### Yanıt Alanları
`playerId`, `playerName`, `teamId`, `teamName`, `country`, `goalsCount`, `homeGoals`, `awayGoals`, `homePenalty`, `awayPenalty`, `matchNum`, `subNum`

---

## 11. FIFA Ranking
**Path:** `GET /sport/football/fifaranking`
**Limit:** 1800 sn/call | Tavsiye: Günde 1
**Plan:** Stats

### Parametreler
| Parametre | Değerler |
|---|---|
| type | "MEN'S RANKING" / "WOMEN'S RANKING" / "Club" |

### Yanıt Alanları
`type`, `teamId`, `teamName`, `continent`, `rank`, `rankChange`, `score`, `scoreChange`, `updateDate`(YYYY-MM)

---

## 12. Player Stats — Match List
**Path:** `GET /sport/football/playerstats/match/list`
**Limit:** 60 sn/call | Tavsiye: 12 saatte 1
**Plan:** Stats

### Yanıt Alanları
`matchId`, `matchTime`, `leagueName`, `homeName`, `awayName`, `modifyTime`

---

## 13. Player Stats — Match Detail
**Path:** `GET /sport/football/playerstats/match`
**Limit:** 10 sn/call | Tavsiye: Dakikada 1
**Plan:** Stats

### Parametreler
| Parametre | Zorunlu | Açıklama |
|---|---|---|
| matchId | Evet | Max 1 hafta geçmiş sorgulanabilir |

### Yanıt Alanları
`playerId`, `teamId`, `number`, `name`, `positionName`, `shots`, `shotsTarget`, `keyPass`, `passRate`, `aerialWon`, `touches`, `dribblesWon`, `wasFouled`, `dispossessed`, `turnOver`, `offsides`, `tackles`, `interception`, `clearances`, `clearanceWon`, `shotsBlocked`, `offsideProvoked`, `fouls`, `totalPass`, `accuratePass`, `crossNum`, `crossWon`, `longBall`, `longBallWon`, `throughBall`, `throughBallWon`, `rating`, `goals`, `assist`, `playingTime`, `red`, `yellow`, `penaltyGoals`, `shotOnPost`, `errorLeadToGoal`, `secondYellow`, `penaltySave`, `firstTeam`(boolean), `isBest`(boolean), `duelTotal`, `aerialTotal`, `highClaims`

---

## 14. European Odds
**Path:** `GET /sport/football/odds/european/all`
**Limit:** 60 sn/call | Tavsiye: Dakikada 1

### Parametreler
`day`, `date`(yyyy-mm-dd), `min`, `matchId`, `companyId`

### Yanıt Alanları
`matchId`, `matchTime`, `leagueName`, `homeName`, `awayName`, `odds`[]: { `oddsId`, `changeTime`, `oddsDetail`, `companyId`, `companyName`, `initialHome`, `initialDraw`, `initialAway`, `instantHome`, `instantDraw`, `instantAway` }

---

## Kodda Kullanılan Endpoint Haritası

| Fonksiyon | Endpoint | Doğru mu? |
|---|---|---|
| getScheduleBasic | /schedule/basic | ✅ |
| getSchedule | /schedule | ✅ |
| getMatchDetail | /schedule?matchId | ✅ |
| getLiveScores | /livescores | ✅ |
| getLiveScoreChanges | /livescores/changes | ✅ |
| getLeagues | /league/basic | ✅ |
| getLeagueTable | /standing/league | ✅ |
| getTeamMatches | /schedule/basic?leagueId → client filtre | ✅ leagueId ile alınır, homeId/awayId==teamId filtrelenir |
| getEvents | /analysis/event | ❓ Doğrulanmadı |
| getStatistics | /analysis/statistics | ❓ Doğrulanmadı |
| getAnalysis | /analysis | ❓ Doğrulanmadı |
| getLineup | /analysis/lineup | ❓ Doğrulanmadı |

---

## Kritik Notlar

1. **Standings'te takım adı ve logo**: `totalStandings` item'larında `name` ve `logo` alanı yoktur. Takım adı ve logosu `leagueInfo.teamInfos` listesinden `teamId` ile eşleştirilerek alınmalıdır. Logo URL `StandingModel.setLogoUrl()` ile set edilir, Glide ile yüklenir.
2. **Takım maçları**: `/schedule/basic` endpoint'i `teamId` parametresini **desteklemiyor**. Çözüm: `leagueId` parametresiyle tüm lig maçları çekilir, ardından client tarafında `homeId == teamId || awayId == teamId` filtresi uygulanır.
3. **API Planları**: `livescores` → Live Data planı; `standing/league`, `topscorer`, `playerstats` → Stats planı gerektirir.
4. **Rate limit**: `livescores/changes` her 15 saniyede bir çağrılabilir (min 10 sn).
5. **BuildConfig.API_KEY**: `local.properties` dosyasındaki `ISPORTS_API_KEY` değeri `build.gradle.kts`'de `Properties()` ile manuel okunmalıdır. `project.findProperty()` `local.properties`'i okumaz.
