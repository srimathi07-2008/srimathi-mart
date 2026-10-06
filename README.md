# Srimathi Mart

A student e-commerce web application focused on home decoration items
and wedding/event flower decorations.

The original front end is preserved: the same `style.css`, the same pages, the
same layout, colours and copy. Only the data layer changed — pages that used to
read `localStorage` or redirect on a hardcoded path now talk to Java servlets
backed by MySQL.

---

## Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Build | Maven (WAR packaging) |
| Server | Apache Tomcat 9.0.x |
| Web tier | Java Servlets (`javax.servlet.*`) |
| Data access | JDBC with `PreparedStatement` throughout |
| Database | MySQL 8 |
| Pooling | HikariCP |
| Passwords | jBCrypt |
| Tests | JUnit 5 with H2 in-memory |

Tomcat 9 is required, not Tomcat 10. Tomcat 10 moved to the `jakarta.servlet.*`
namespace and this project targets `javax.servlet.*` as specified.

---

## Architecture

```
com.srimathi.srimathimart
├── controller   Servlets. No SQL, no business rules. Read params, call a
│                service, serialise the result.
├── service      Business rules, validation, ownership checks, transactions.
├── dao          Persistence only. Every statement is a PreparedStatement.
├── model        Entities mapped to tables.
├── dto          Request/response shapes, including the password-free
│                SessionUser stored in HttpSession.
├── filter       EncodingFilter (UTF-8), AuthFilter (route protection).
├── listener     DataSourceListener — the only owner of the HikariCP pool.
├── util         Config, Db, Json, HttpUtil, PasswordUtil.
└── exception    Typed failures mapped to HTTP statuses in BaseServlet.
```

Rules the code holds to:

- **No SQL in controllers.** Grep `controller/` for `SELECT` — there is none.
- **No business logic in DAOs.** Ownership, validation and totals live in
  services; DAOs only read and write rows.
- **No string-concatenated SQL.** The one query with a dynamic shape
  (`JdbcProductDao.search`) assembles its `WHERE` clause from fixed literal
  fragments and binds the user's keyword and category as parameters.
- **One connection pool.** `DataSourceListener` creates and closes it;
  everything else borrows through `util.Db`. `DriverManager.getConnection()`
  appears nowhere.
- **Passwords are never stored in plaintext.** jBCrypt only, and the hash never
  enters the session.

---

## HTTP endpoints

| Method | Path | Who | Purpose |
|---|---|---|---|
| POST | `/api/register` | public | Buyer or seller signup |
| POST | `/api/login` | public | Sign in (role-aware) |
| GET/POST | `/api/logout` | any | End the session |
| GET | `/api/session` | any | Who am I |
| GET | `/api/products` | public | Browse, `?q=` search, `?category=` filter |
| GET | `/api/categories` | public | Distinct categories |
| GET/POST | `/api/seller/products` | SELLER | List / create / update / delete |
| GET/POST | `/api/cart` | BUYER | View / add / update / remove |
| POST | `/api/checkout` | BUYER | Place order (mock payment) |
| GET | `/api/orders` | BUYER | Order history |
| GET/POST | `/api/reviews` | public/BUYER | View and submit product reviews and ratings |
| POST | `/api/chat` | public | AI assistant for decoration and website help |

---

## 1. Prerequisites

```bash
java -version     # must report 17
mvn -version
mysql --version   # MySQL 8
```

Download Tomcat **9.0.x** from https://tomcat.apache.org/download-90.cgi and
unpack it. Set `CATALINA_HOME` to that directory.

```bash
export CATALINA_HOME=/path/to/apache-tomcat-9.0.xx
```

---

## 2. Connect MySQL

Start MySQL, then create the schema and an application user:

```bash
mysql -u root -p < db/schema.sql
```

That creates the `srimathi_mart` database and all six tables: `users`,
`products`, `orders`, `order_items`, `cart_items`, `reviews`.

Create a dedicated user rather than using root:

```sql
CREATE USER 'srimathi_app'@'localhost' IDENTIFIED BY 'your-strong-password';
GRANT SELECT, INSERT, UPDATE, DELETE ON srimathi_mart.* TO 'srimathi_app'@'localhost';
FLUSH PRIVILEGES;
```

Load the sample catalogue:

```bash
mysql -u root -p srimathi_mart < db/seed.sql
```

`seed.sql` is re-runnable — every statement is `INSERT IGNORE` against a
`UNIQUE` constraint.

### Configuration

```bash
cp src/main/resources/config.properties.example src/main/resources/config.properties
```

Edit it and set at least:

```properties
db.url=jdbc:mysql://localhost:3306/srimathi_mart?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
db.username=srimathi_app
db.password=your-strong-password

seed.admin.password=your-admin-password
seed.seller.password=your-seller-password
seed.buyer.password=your-buyer-password
```

`config.properties` is in `.gitignore` and must never be committed. Every key
can instead be supplied as an environment variable (dots become underscores,
upper case) or a `-D` system property, both of which take precedence:

```bash
export DB_URL='jdbc:mysql://localhost:3306/srimathi_mart?useSSL=false&serverTimezone=UTC'
export DB_USERNAME=srimathi_app
export DB_PASSWORD='your-strong-password'
export SEED_ADMIN_PASSWORD='your-admin-password'
```

### How seed passwords work

`seed.sql` inserts the three accounts with an **empty** `password_hash`. An
empty string is not a valid bcrypt hash, so nothing can log in as those rows as
they stand.

On first startup `SeedService` hashes the passwords from your configuration
with jBCrypt and fills the blanks in. No credential — not even a hash — is ever
committed to the repository.

Seed accounts:

| Role | Email | Password |
|---|---|---|
| Admin | `admin@srimathimart.com` | `seed.admin.password` |
| Seller | `seller@srimathimart.com` | `seed.seller.password` |
| Buyer | `buyer@srimathimart.com` | `seed.buyer.password` |

There is **no admin signup**. `AuthService.register` rejects `role=ADMIN` even
if posted directly to `/api/register`. The seed listener is the only way an
administrator account comes into being. Set `seed.demo.enabled=false` to skip
the demo buyer and seller.

---

## 3. Build

```bash
mvn clean package
```

Produces `target/srimathi-mart.war`.

Run the tests on their own:

```bash
mvn test
```

The tests use H2 in memory — no MySQL needed, nothing is written to disk.

Static analysis (optional, non-blocking):

```bash
mvn checkstyle:check
mvn spotbugs:check
```

---

## 4. Deploy locally

### Option A — copy the WAR (simplest)

```bash
mvn clean package
cp target/srimathi-mart.war $CATALINA_HOME/webapps/

# Linux / macOS
$CATALINA_HOME/bin/startup.sh

# Windows
%CATALINA_HOME%\bin\startup.bat
```

Open **http://localhost:8080/srimathi-mart/**

Watch the log to confirm the pool started and the seeding ran:

```bash
tail -f $CATALINA_HOME/logs/catalina.out
```

You are looking for `Srimathi Mart: HikariCP pool started.` and one
`Seed account created:` line per account.

Stop with `$CATALINA_HOME/bin/shutdown.sh`.

### Option B — Maven Cargo plugin

With `CATALINA_HOME` exported:

```bash
mvn clean package cargo:run
```

Serves at the same URL and redeploys on rebuild.

### Option C — IDE

Add a Tomcat 9 run configuration, point it at the project, set the context path
to `/srimathi-mart`, and run.

---

## 5. Test registration and login

### Through the UI

1. Go to http://localhost:8080/srimathi-mart/
2. **Buyer:** click *Continue Shopping* → *Create an account* → fill in
   `register.html` → you land on `home.html` already signed in.
3. **Seller:** click *Enter Seller Space* → *Create a seller account* →
   `seller-register.html` → you land on `seller-dashboard.html`.
4. **Admin:** click *Open Admin Panel* and use the seeded admin credentials.
   There is no admin signup link, by design.

### Full happy path

1. Sign in as the seller. Go to *Add product* (`seller.html`), create one with a
   name, price, stock, category and a suitable decoration image.
2. Sign out, sign in as the buyer, open `home.html` — the new product appears in
   the grid. Try the search box and a category tile.
3. *Add to Cart*, open the cart, change the quantity, remove a line.
4. *Proceed to Checkout* → a mock payment reference and `order-confirmation.html`.
5. Sign back in as the seller — the dashboard stock has dropped.

### From the command line

```bash
BASE=http://localhost:8080/srimathi-mart

# Register a buyer
curl -i -c jar.txt -X POST $BASE/api/register \
  -H 'X-Requested-With: XMLHttpRequest' \
  -d 'fullName=Test Buyer' \
  -d 'email=test@example.com' \
  -d 'password=Password@123' \
  -d 'confirmPassword=Password@123' \
  -d 'role=BUYER'

# Sign in
curl -i -c jar.txt -X POST $BASE/api/login \
  -H 'X-Requested-With: XMLHttpRequest' \
  -d 'email=test@example.com' -d 'password=Password@123' -d 'role=BUYER'

# Who am I
curl -b jar.txt -H 'X-Requested-With: XMLHttpRequest' $BASE/api/session

# Browse, search, filter
curl "$BASE/api/products"
curl "$BASE/api/products?q=headphones"
curl "$BASE/api/products?category=Electronics"

# Cart and checkout
curl -b jar.txt -X POST $BASE/api/cart \
  -H 'X-Requested-With: XMLHttpRequest' \
  -d 'action=add' -d 'productId=1' -d 'quantity=2'
curl -b jar.txt -X POST $BASE/api/checkout -H 'X-Requested-With: XMLHttpRequest'
```

Registering as an admin must fail:

```bash
curl -i -X POST $BASE/api/register -H 'X-Requested-With: XMLHttpRequest' \
  -d 'fullName=Hacker' -d 'email=x@example.com' \
  -d 'password=Password@123' -d 'confirmPassword=Password@123' -d 'role=ADMIN'
# expect 400 - "Administrator accounts cannot be self registered."
```

---

## Security notes

- **bcrypt** at cost 12 by default, with a per-account random salt.
- **Session fixation** — the session id is regenerated on every successful
  login and registration.
- **Session timeout** — 30 minutes, set in `web.xml` and again per session from
  `session.timeout.seconds`.
- **HttpOnly** session cookie. Set `<secure>true</secure>` in `web.xml` once you
  serve over HTTPS.
- **Account enumeration** — a wrong password and an unknown email return an
  identical message, and an unknown email still performs a bcrypt comparison so
  the timing matches.
- **Ownership** — a seller can only edit or delete their own products; a buyer
  id always comes from the session, never from a request parameter.
- **Output escaping** — the JSON writer escapes `<`, `>` and `&`, and the front
  end escapes again before touching `innerHTML`.
- **Payment is mocked.** No gateway is contacted and no card details are
  accepted or stored anywhere in this project.

---

## Troubleshooting

| Symptom | Cause |
|---|---|
| `Missing required configuration: db.url` at startup | `config.properties` not created, or not on the classpath. Rebuild after creating it. |
| `Communications link failure` | MySQL is not running, or the port in `db.url` is wrong. |
| `Public Key Retrieval is not allowed` | Add `allowPublicKeyRetrieval=true` to `db.url`. |
| Seed accounts cannot sign in | Check the log for `Seed account created`. If `schema.sql` had not run, seeding was skipped — run it and restart. |
| `ClassNotFoundException: javax.servlet.http.HttpServlet` | You are on Tomcat 10. Use Tomcat 9.0.x. |
| Product grid is empty | Run `db/seed.sql`, or add a product as the seller. |
| Rupee sign or emoji shows as `?` | The database is not `utf8mb4`, or `characterEncoding=UTF-8` is missing from `db.url`. |

---

## Commit convention

`feat:` new feature · `fix:` bug fix · `test:` tests · `docs:` documentation
