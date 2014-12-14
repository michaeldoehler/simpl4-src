class Presenter {
    String name
    def buttonClick() {
        if (name) {
            $('#salutes').append("<p>Hello ${name}!</p>")
        }
    }
}
