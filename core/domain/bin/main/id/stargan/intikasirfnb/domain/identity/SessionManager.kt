package id.stargan.intikasirfnb.domain.identity

interface SessionManager {
    fun setCurrentUser(user: User)
    fun setCurrentOutlet(outlet: Outlet)
    fun getCurrentUser(): User?
    fun getCurrentOutlet(): Outlet?
    fun logout()
}
