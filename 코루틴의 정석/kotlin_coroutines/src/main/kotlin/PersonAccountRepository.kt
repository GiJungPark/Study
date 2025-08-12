interface PersonAccountRepository {
    suspend fun searchByName(name: String): Array<Follower.PersonAccount>
}