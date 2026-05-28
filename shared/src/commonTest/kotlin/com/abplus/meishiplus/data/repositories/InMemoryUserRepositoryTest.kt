package com.abplus.meishiplus.data.repositories

import com.abplus.meishiplus.data.entities.UserEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class InMemoryUserRepositoryTest {
    @Test
    fun addUser_assignsIdWhenBlank() = runTest {
        val repository = InMemoryUserRepository()

        val added = repository.addUser(UserEntity())

        assertTrue(added.id.isNotBlank())
        assertEquals(added, repository.getUser(added.id))
    }

    @Test
    fun addUser_preservesExplicitIdAndCards() = runTest {
        val repository = InMemoryUserRepository()
        val user = UserEntity(
            id = "user-1",
            cardIds = listOf("card-1", "card-2"),
        )

        val added = repository.addUser(user)

        assertEquals("user-1", added.id)
        assertEquals(listOf("card-1", "card-2"), added.cardIds)
        assertEquals(added, repository.getUser("user-1"))
    }

    @Test
    fun saveUpdateAndDeleteUser_mutateStoredUser() = runTest {
        val repository = InMemoryUserRepository()
        repository.saveUser(UserEntity(id = "user-1", cardIds = listOf("card-1")))

        assertEquals(listOf("card-1"), repository.getUser("user-1").cardIds)

        repository.updateUser(UserEntity(id = "user-1", cardIds = listOf("card-2")))
        assertEquals(listOf("card-2"), repository.getUser("user-1").cardIds)

        repository.deleteUser("user-1")
        assertFailsWith<IllegalStateException> {
            repository.getUser("user-1")
        }
    }

    @Test
    fun getUser_failsWhenUserDoesNotExist() = runTest {
        val repository = InMemoryUserRepository()

        val error = assertFailsWith<IllegalStateException> {
            repository.getUser("missing")
        }
        assertEquals("User not found: missing", error.message)
    }
}

private class InMemoryUserRepository : UserRepository {
    private val users = mutableMapOf<String, UserEntity>()
    private var nextId = 0

    override suspend fun addUser(user: UserEntity): UserEntity {
        val id = user.id.ifBlank { "user-${nextId++}" }
        val userWithId = user.copy(id = id)
        users[id] = userWithId
        return userWithId
    }

    override suspend fun getUser(id: String): UserEntity =
        users[id] ?: error("User not found: $id")

    override suspend fun saveUser(user: UserEntity) {
        users[user.id] = user
    }

    override suspend fun deleteUser(id: String) {
        users.remove(id)
    }

    override suspend fun updateUser(user: UserEntity) {
        saveUser(user)
    }
}
