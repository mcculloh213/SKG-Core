package ktx.sovereign.core.contract

interface BaseView<T> {
    fun registerPresenter(presenter: T)
}