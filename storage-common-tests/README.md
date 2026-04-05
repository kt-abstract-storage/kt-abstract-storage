# storage-common-tests

Reusable, publishable test contracts for `kt-abstract-storage` implementations.

## What it provides

- Cross-platform test contracts in `commonMain`
- Contracts that depend on `:commonLib` abstractions
- A Maven-publishable artifact: `kt-abstract-storage-common-tests`

## Contract classes

- `CommonFileTests`
- `CommonFolderTests`
- `CommonModifiableFolderTests`

## Usage

Add the dependency in a test source set, then implement fixture factories in your test class.

```kotlin
class MyStorageTests : CommonModifiableFolderTests() {
    override suspend fun createModifiableFolderFixtureAsync(): ModifiableFolderFixture {
        val root = /* create your ModifiableFolder */
        return ModifiableFolderFixture(root = root, cleanup = { /* optional cleanup */ })
    }

    override suspend fun createModifiableFolderWithItemsFixtureAsync(
        fileCount: Int,
        folderCount: Int,
    ): ModifiableFolderFixture {
        val root = /* create and pre-populate */
        return ModifiableFolderFixture(root = root)
    }

    @Test
    fun all_common_contracts() = runTest {
        runAllCommonModifiableFolderContracts()
    }
}
```

