package dev.amaro.sonic

interface IRenderer<T> {
    fun render(state: T, performer: IPerformer<T>)
    class Nothing<T> : IRenderer<T> {
        override fun render(state: T, performer: IPerformer<T>) = Unit
    }
}