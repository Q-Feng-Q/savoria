# Family Wallet Lazy Initialization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate `/family/wallet` 404 responses caused by missing wallet rows by safely creating one zero-balance account for the current active family on first use.

**Architecture:** Add an idempotent mapper insert constrained to active families and a separate proxied `REQUIRES_NEW` initializer so missing-row creation commits outside monetary lock transactions. `FamilyWalletServiceImpl` will retry reads after initialization and will call the initializer before every wallet row lock, preserving the existing lock order for already-created accounts.

**Tech Stack:** Java 17, Spring Boot 3, Spring transactions, MyBatis XML, JUnit 5, Mockito, Testcontainers MySQL 8.

---

### Task 1: Add the idempotent account initializer

**Files:**
- Modify: `backend/src/main/java/com/familykitchen/wallet/mapper/FamilyWalletMapper.java`
- Modify: `backend/src/main/resources/mapper/wallet/FamilyWalletMapper.xml`
- Create: `backend/src/main/java/com/familykitchen/wallet/service/FamilyWalletAccountInitializer.java`
- Create: `backend/src/test/java/com/familykitchen/wallet/FamilyWalletAccountInitializerTest.java`
- Create: `backend/src/test/java/com/familykitchen/wallet/FamilyWalletInitializationSqlContractTest.java`

- [ ] **Step 1: Write failing initializer and SQL contract tests**

Test that an existing account performs only `selectAccount`, a missing account invokes `insertAccountIfMissing`, and the public `ensure(long)` method has `@Transactional(propagation = Propagation.REQUIRES_NEW)`.

Test the mapper XML contains an `insertAccountIfMissing` statement using `insert ignore`, initializes both amounts to zero, selects from `families`, and restricts `status='active'`.

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -f backend/pom.xml -Dtest=FamilyWalletAccountInitializerTest,FamilyWalletInitializationSqlContractTest test
```

Expected: compilation/test failure because the initializer and mapper method do not exist.

- [ ] **Step 3: Add the mapper method and SQL**

Add:

```java
int insertAccountIfMissing(@Param("familyId") long familyId);
```

Map it to:

```xml
<insert id="insertAccountIfMissing">
  insert ignore into family_wallets(family_id,available_amount,frozen_amount)
  select id,0,0 from families where id=#{familyId} and status='active'
</insert>
```

- [ ] **Step 4: Add the isolated initializer**

Create a Spring `@Service` with constructor-injected `FamilyWalletMapper`:

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void ensure(long familyId) {
  if (mapper.selectAccount(familyId) == null) {
    mapper.insertAccountIfMissing(familyId);
  }
}
```

- [ ] **Step 5: Run focused tests and verify GREEN**

Run the command from Step 2. Expected: all focused tests pass.

- [ ] **Step 6: Commit Task 1**

```powershell
git add backend/src/main/java/com/familykitchen/wallet/mapper/FamilyWalletMapper.java backend/src/main/resources/mapper/wallet/FamilyWalletMapper.xml backend/src/main/java/com/familykitchen/wallet/service/FamilyWalletAccountInitializer.java backend/src/test/java/com/familykitchen/wallet/FamilyWalletAccountInitializerTest.java backend/src/test/java/com/familykitchen/wallet/FamilyWalletInitializationSqlContractTest.java
git commit -m "feat: initialize missing family wallets safely"
```

### Task 2: Integrate initialization with reads and wallet locks

**Files:**
- Modify: `backend/src/main/java/com/familykitchen/wallet/service/impl/FamilyWalletServiceImpl.java`
- Modify: `backend/src/test/java/com/familykitchen/wallet/FamilyWalletServiceTest.java`

- [ ] **Step 1: Write failing service tests**

Add a mocked `FamilyWalletAccountInitializer` to the service constructor in every test. Cover:

- existing `get` returns without invoking initializer;
- missing `get` invokes initializer and returns the second selected zero account;
- missing `get` still throws the existing `NOT_FOUND` error when the second select is null;
- every wallet lock path calls `initializer.ensure(familyId)` before `mapper.lockAccount(familyId)`.

- [ ] **Step 2: Run the service test and verify RED**

```powershell
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -f backend/pom.xml -Dtest=FamilyWalletServiceTest test
```

Expected: compilation/assertion failure because the service does not depend on or call the initializer.

- [ ] **Step 3: Implement read retry and pre-lock ensure**

Inject `FamilyWalletAccountInitializer`. Change `get` to select, ensure only when missing, then select again. Change private `lockAccount` to call `initializer.ensure(id)` before the existing `mapper.lockAccount(id)`. Preserve the original `NOT_FOUND` error if no row exists afterward.

- [ ] **Step 4: Run focused wallet tests and verify GREEN**

```powershell
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -f backend/pom.xml -Dtest=FamilyWalletServiceTest,FamilyWalletAccountInitializerTest,FamilyWalletInitializationSqlContractTest,FamilyWalletControllerContractTest test
```

Expected: all focused wallet tests pass.

- [ ] **Step 5: Commit Task 2**

```powershell
git add backend/src/main/java/com/familykitchen/wallet/service/impl/FamilyWalletServiceImpl.java backend/src/test/java/com/familykitchen/wallet/FamilyWalletServiceTest.java
git commit -m "fix: recover missing family wallet accounts"
```

### Task 3: Prove first-use and steady-state concurrency

**Files:**
- Modify: `backend/src/test/java/com/familykitchen/wallet/FamilyWalletConcurrencyMySqlTest.java`
- Test: `backend/src/test/java/com/familykitchen/common/security/IdentityContextMapperContractTest.java`

- [ ] **Step 1: Write the MySQL concurrency regressions**

Ensure the fixture creates active family `7001`. Add tests that:

- delete its wallet, race two `wallets.get(7001)`, require two successes and one final wallet row;
- delete its wallet, race two distinct `manualCredit` commands, require no deadlock/SQL state `40001`, one final wallet row, and the exact sum of both credits;
- retain and run `twoFreezesCannotOverdrawOneFamilyBalance` to prove existing-wallet serialization remains safe.

Keep the identity mapper contract assertion that `family_id` comes from active `families f`, not directly from the family relation.

- [ ] **Step 2: Run the Testcontainers test**

```powershell
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -f backend/pom.xml -Dtest=FamilyWalletConcurrencyMySqlTest,IdentityContextMapperContractTest test
```

Expected with Docker available: all tests pass. If Docker is unavailable, Testcontainers may skip the MySQL class; report the skip explicitly and do not run these tests against the configured business database.

- [ ] **Step 3: Run the complete backend suite**

```powershell
& 'D:\develop\apache-maven-3.9.9\bin\mvn.cmd' -f backend/pom.xml test
```

Expected: complete backend suite passes with zero failures; Docker-disabled skips are reported accurately.

- [ ] **Step 4: Inspect scope and verify no migration was executed**

Run `git diff --check` and inspect `git status --short`. Confirm no migration runner command, bulk SQL, or current business database mutation was executed.

- [ ] **Step 5: Commit Task 3**

```powershell
git add backend/src/test/java/com/familykitchen/wallet/FamilyWalletConcurrencyMySqlTest.java backend/src/test/java/com/familykitchen/common/security/IdentityContextMapperContractTest.java
git commit -m "test: cover family wallet first-use concurrency"
```
