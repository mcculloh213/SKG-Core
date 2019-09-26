package ktx.sovereign.html

interface Element {
    fun render(builder: StringBuilder, indent: String)
}

class TextElement(val text: String) : Element {
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent$text\n")
    }
}

@DslMarker
annotation class HtmlTagMarker

@HtmlTagMarker
abstract class Tag(val name: String) : Element {
    val children = arrayListOf<Element>()
    val attributes = hashMapOf<String, String>()

    protected fun <T : Element> initTag(tag: T, init: T.() -> Unit): T {
        tag.init()
        children.add(tag)
        return tag
    }
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent<$name${renderAttributes()}>\n")
        for (c in children) {
            c.render(builder, "$indent  ")
        }
        builder.append("$indent</$name>\n")
    }
    private fun renderAttributes(): String {
        val builder = StringBuilder()
        for ((attr, value) in attributes) {
            builder.append(" $attr=\"$value\"")
        }
        return builder.toString()
    }
    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }
}

abstract class TagWithText(name: String) : Tag(name) {
    operator fun String.unaryPlus() {
        children.add(TextElement(this))
    }
    operator fun String.unaryMinus() {
        attributes["style"] += this
    }
}

class HTML : TagWithText("html") {
    fun head(init: Head.() -> Unit) = initTag(Head(), init)
    fun body(init: Body.() -> Unit) = initTag(Body(), init)
}
class Head : TagWithText("head") {
    fun title(init: Title.() -> Unit) = initTag(Title(), init)
    fun style(init: Style.() -> Unit) = initTag(Style(), init)
    fun script(init: Script.() -> Unit) = initTag(Script(), init)
}
class Title : TagWithText("title")
class Style : TagWithText("style") {
    fun selector(name: String, init: Selector.() -> Unit): Element {
        val selector = Selector()
        selector.init()
        children.add(selector)
        selector.name = name
        return selector
    }
}
class Selector : Element {
    var name: String = ""
    val properties = arrayListOf<Element>()
    fun property(init: Property.() -> Unit) = initProperty(Property(), init)
    private fun <P : Element> initProperty(property: P, init: P.() -> Unit): P {
        property.init()
        properties.add(property)
        return property
    }
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent$name {\n")
        for (p in properties) {
            p.render(builder, "$indent  ")
        }
        builder.append("$indent}\n")
    }
    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }
}
class Property : Element {
    var prop: String = ""
    var value: String = ""
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent$prop: $value;\n")
    }
    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }
}
class Script : TagWithText("script") {
    fun variable(init: Variable.() -> Unit) {
        val variable = Variable()
        variable.init()
        children.add(variable)
    }
    fun function(init: Function.() -> Unit): Element {
        val function = Function()
        function.init()
        children.add(function)
        return function
    }
}
class Variable : Element {
    var name: String = ""
    var value: String? = null
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("${indent}var $name")
        if (!value.isNullOrEmpty()) { builder.append(" = $value") }
        builder.append(";\n")
    }
}
class VariableReference : Element {
    var name: String = ""
    var value: String? = null
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent$name")
        if (!value.isNullOrEmpty()) { builder.append(" = $value") }
        builder.append(";\n")
    }
}
class Function : Element {
    val decl = FunctionDeclaration()
    val body = FunctionBody()
    fun declaration(name: String, vararg params: String) {
        decl.name = name
        decl.params.addAll(params)
    }
    fun body(init: FunctionBody.() -> Unit): Element {
        body.init()
        return body
    }
    override fun render(builder: StringBuilder, indent: String) {
        decl.render(builder, indent)
        body.render(builder, "$indent  ")
        builder.append("}\n")
    }
}
class FunctionDeclaration : Element {
    var name: String = ""
    val params: MutableList<String> = mutableListOf()
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("${indent}function $name(")
        var comma = params.size - 1
        params.forEach {
            builder.append(it)
            if (comma > 0) {
                builder.append(",")
                comma -= 1
            }
        }
        builder.append(") {\n")
    }
}
open class FunctionBody : Element {
    val lines = arrayListOf<Element>()
    fun variable(init: Variable.() -> Unit) {
        val variable = Variable()
        variable.init()
        lines.add(variable)
    }
    fun call(line: String) {
        lines.add(TextElement(line))
    }
    fun reference(init: VariableReference.() -> Unit) {
        val ref = VariableReference()
        ref.init()
        lines.add(ref)
    }
    fun doIf(condition: String, init: If.() -> Unit): Element {
        val conditional = If()
        conditional.init()
        conditional.condition = condition
        lines.add(conditional)
        return conditional
    }
    override fun render(builder: StringBuilder, indent: String) {
        lines.forEach {
            it.render(builder, indent)
        }
    }
}
class If : FunctionBody() {
    var condition: String = "true"
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("${indent}if ($condition) {\n")
        lines.forEach {
            it.render(builder, "$indent  ")
        }
        builder.append("}\n")
    }
}
abstract class BodyTag(name: String) : TagWithText(name) {
    var className: String
        get() = attributes["class"] ?: ""
        set(value) {
            attributes["class"] = value
        }
    var id: String
        get() = attributes["id"] ?: ""
        set(value) {
            attributes["id"] = value
        }

    fun h1(init: H1.() -> Unit) = initTag(H1(), init)
    fun h2(init: H2.() -> Unit) = initTag(H2(), init)
    fun h3(init: H3.() -> Unit) = initTag(H3(), init)
    fun h4(init: H4.() -> Unit) = initTag(H4(), init)
    fun h5(init: H5.() -> Unit) = initTag(H5(), init)
    fun h6(init: H6.() -> Unit) = initTag(H6(), init)
    fun a(href: String, init: A.() -> Unit) {
        val a = initTag(A(), init)
        a.href = href
    }
    fun b(init: B.() -> Unit) = initTag(B(), init)
    fun p(init: P.() -> Unit) = initTag(P(), init)
    fun span(init: Span.() -> Unit) = initTag(Span(), init)
    fun div(init: Div.() -> Unit) = initTag(Div(), init)
    fun br(init: Br.() -> Unit) = initTag(Br(), init)

}
class Body : BodyTag("body")
class B : BodyTag("b")
class P : BodyTag("p")
class H1 : BodyTag("h1")
class H2 : BodyTag("h2")
class H3 : BodyTag("h3")
class H4 : BodyTag("h4")
class H5 : BodyTag("h5")
class H6 : BodyTag("h6")
class A : BodyTag("a") {
    var href: String
        get() = attributes["href"]!!
        set(value) {
            attributes["href"] = value
        }
}
class Div : BodyTag("div")
class Span : BodyTag("span")
class Br : BodyTag("br")

fun html(init: HTML.() -> Unit): HTML {
    val html = HTML()
    html.init()
    return html
}