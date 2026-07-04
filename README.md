# MinIO Storage App

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-backend-brightgreen?logo=springboot&logoColor=white)
![MinIO](https://img.shields.io/badge/MinIO-object%20storage-C72E49?logo=minio&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-database-4479A1?logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)

Backend sistem koji integriše Spring Boot sa MinIO objektnim skladištem i MySQL bazom podataka za metapodatke. Sistem funkcioniše kao posrednički sloj između klijenta i S3-kompatibilnog skladišta – apstrahuje direktnu komunikaciju sa skladištem i centralizuje poslovnu logiku, kontrolu pristupa i upravljanje metapodacima.

## Sadržaj

- [Arhitektura](#arhitektura)
- [Tehnologije](#tehnologije)
- [Struktura projekta](#struktura-projekta)
- [Preduslovi](#preduslovi)
- [Pokretanje aplikacije](#pokretanje-aplikacije)
- [Pregled funkcionalnosti](#pregled-funkcionalnosti)
- [Sigurnost](#sigurnost)

## Arhitektura

Sistem je organizovan kao troslojna arhitektura: **klijentski sloj** (Swagger UI za testiranje REST API-ja), **aplikacioni sloj** (Spring Boot – kontroleri, poslovna logika, pristup podacima) i **infrastrukturni sloj** (MySQL i MinIO, oba u Docker kontejnerima).

![Arhitektura sistema](docs/architecture-diagram.png)

Ključna arhitektonska odluka je razdvajanje **metapodataka** (čuvaju se u MySQL bazi) od **binarnog sadržaja** (skladišti se na MinIO serveru). Aplikacioni sloj koordinira oba sistema i obezbjeđuje njihovu međusobnu konzistentnost kroz strog redoslijed operacija i mehanizme kompenzacije u slučaju parcijalnog neuspjeha.

Ostali dizajnerski principi na kojima se sistem zasniva:

- **Baza kao primarni izvor metapodataka** – čitanje, pretraga i filtriranje idu isključivo kroz bazu; MinIO se poziva samo kada je potreban binarni sadržaj ili status verzioniranja
- **Granularna kontrola pristupa na nivou fajla** – aplikacioni sloj nadograđuje MinIO-ovu kontrolu na nivou bucketa detaljnijim politikama
- **Stateless streaming pristup** – aplikacija ne čuva fajlove lokalno niti ih učitava u memoriju, već ih kontinuiranim tokom prenosi direktno na/sa MinIO servera
- **Kompenzacioni mehanizmi** – budući da baza i MinIO ne dijele zajednički transakcioni kontekst, neuspjeh u jednom sistemu se automatski sanira u drugom (npr. rollback otpremljenog objekta ako upis u bazu ne uspije)

## Tehnologije

| Kategorija | Tehnologija |
|---|---|
| Jezik / Runtime | Java 17 |
| Framework | Spring Boot |
| Autentifikacija | JWT (access + refresh token) |
| Baza podataka | MySQL |
| Objektno skladište | MinIO (S3-kompatibilno) |
| Dokumentacija API-ja | Swagger / OpenAPI |
| Build alat | Apache Maven |
| Infrastruktura | Docker, Docker Compose |

## Struktura projekta

```
minio-storage-app/
├── backend-app/          # Spring Boot aplikacija
│   ├── src/
│   └── pom.xml
└── docker-compose.yml    # MinIO, MySQL, phpMyAdmin
```

## Preduslovi

Za pokretanje aplikacije potrebno je imati instalirano:

- Java 17
- Apache Maven
- Docker i Docker Compose

## Pokretanje aplikacije

### 1. Pokretanje Docker servisa

Sve infrastrukturne komponente pokreću se jednom komandom iz root foldera projekta:

```bash
cd minio-storage-app
docker compose up -d
```

Nakon toga dostupni su sljedeći servisi:

| Servis | URL | Korisnik | Lozinka |
|---|---|---|---|
| MinIO Konzola | http://localhost:9001 | minioadmin | minioadmin123 |
| MinIO API | http://localhost:9000 | minioadmin | minioadmin123 |
| phpMyAdmin | http://localhost:8081 | root | root123 |
| MySQL | localhost:3306 | root / appuser | root123 / appuser123 |

Baza podataka koja se koristi: `minio_app`

### 2. Inicijalizacija administratorskog korisnika

Prije prvog pokretanja aplikacije potrebno je kreirati inicijalnog administratorskog korisnika direktno u bazi podataka:

1. Otvoriti phpMyAdmin na adresi [http://localhost:8081](http://localhost:8081) (korisnik `root`, lozinka `root123`)
2. Odabrati bazu `minio_app`, zatim tabelu `users`
3. Izvršiti sljedeći SQL upit:

```sql
INSERT INTO users (
    username,
    email,
    password,
    role,
    is_active,
    is_email_verified,
    created_at
) VALUES (
    'system',
    'system@storage.com',
    '$2a$10$6Ce95JzzT7BGz5mTOTDKiOmwHdx85oIy2ySrTHVokdef/vCrdYg3a',
    'ADMIN',
    1,
    1,
    NOW()
);
```

> Ovaj korak zaobilazi standardnu registraciju i email verifikaciju, te direktno kreira korisnika s administratorskom rolom.
> **Korisničko ime:** `system` &nbsp;·&nbsp; **Lozinka:** `system123*`

### 3. Pokretanje Spring Boot aplikacije

Iz foldera `backend-app`, pokrenuti aplikaciju sljedećom komandom:

```bash
cd backend-app
mvn spring-boot:run
```

Sve potrebne zavisnosti definisane su u `pom.xml` fajlu i biće automatski preuzete prilikom prvog pokretanja.

### 4. Testiranje API-ja putem Swagger UI

Nakon što je aplikacija pokrenuta, kompletna API dokumentacija dostupna je na adresi:
[http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html)

**Autentifikacija:**

1. Pronaći endpoint `POST /auth/login`
2. Unijeti kredencijale:
   ```json
   { "username": "system", "password": "system123*" }
   ```
3. Iz odgovora kopirati dobijeni JWT token
4. Kliknuti na dugme **Authorize** (u gornjem desnom uglu Swagger interfejsa)
5. Unijeti token u formatu: `Bearer <token>`

Nakon toga svi zaštićeni endpointi su dostupni za testiranje.

## Pregled funkcionalnosti

Aplikacija je organizovana kroz 12 kontrolera, svaki zadužen za jasno razgraničen dio sistema:

| Kontroler | Odgovornost |
|---|---|
| **AuthController** | Registracija sa email verifikacijom, prijava (korisničko ime ili email), JWT sa rotacijom refresh tokena, odjava, oporavak lozinke, logovanje kritičnih događaja |
| **UserController** | Administracija naloga – dodjela uloga, aktivacija/deaktivacija, zaštita sistemskog naloga, garancija bar jednog aktivnog admina, soft-delete |
| **HealthController** | Liveness i readiness provjere, status baze i MinIO servisa (UP/DEGRADED/DOWN), detaljne metrike za administratore |
| **BucketController** | Kreiranje, brisanje i promjena vidljivosti bucketa, provjera dostupnosti naziva, uvid prilagođen ulozi korisnika, uključivanje verzioniranja |
| **FileCommandController** | Otpremanje fajlova streaming pristupom, brisanje, ažuriranje metapodataka; atomična provjera kvote i rollback pri neuspjehu |
| **FileDownloadController** | Preuzimanje i pregled fajlova – presigned URL-ovi i proxy streaming, sanitizacija naziva, podrška za Unicode |
| **FileQueryController** | Read-only pregled – detalji fajla, lična kolekcija, javni katalog, filtriranje, napredna pretraga (POST) sa paginacijom |
| **FileStatisticsController** | Statistika iskorištenosti prostora po korisniku, bucketu i kategoriji; admin uvid u najveće potrošače |
| **ShareLinkController** | Javno dijeljenje fajlova putem privremenih tokena bez autentifikacije, rokovi važenja, brojač posjeta, deaktivacija/brisanje |
| **FileVersionController** | Istorija verzija fajla, vraćanje starije verzije, brisanje verzije (isključivo admin), nasljeđivanje vidljivosti od roditeljskog fajla |
| **BucketLifecycleController** | Lifecycle pravila (automatsko brisanje/premještanje) i retention politika zasnovana na Object Lock-u (GOVERNANCE/COMPLIANCE) |
| **BucketAdminController** | Sinhronizacija baze i MinIO servera, revizija siročića i fantomskih zapisa, rekalkulacija statistike bucketa |

## Sigurnost

- JWT autentifikacija sa dvostrukim tokenom (access + refresh) i rotacijom refresh tokena; ponovna upotreba iskorišćenog tokena opoziva aktivne sesije
- Verifikacioni ključevi i tokeni za reset lozinke čuvaju se u bazi isključivo u obliku SHA-256 heša
- Granularna kontrola pristupa na nivou fajla, bucketa i uloge (vlasnik / vlasnik bucketa / administrator / javno)
- Presigned URL-ovi sa ograničenim trajanjem (podrazumijevano 900s, maksimalno 7200s)
- Sanitizacija naziva fajlova prilikom preuzimanja radi sprječavanja napada putem zaglavlja

## Autor

**Svetozar Vuković** — [GitHub](https://github.com/VukovicSvetozar)
