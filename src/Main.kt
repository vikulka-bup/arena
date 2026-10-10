sealed class Damage(val amount: Int) {
    init {
        require(amount >= 0) { "Урон не может быть отрицательным" }
    }

    class Physical(amount: Int) : Damage(amount)
    class Magical(amount: Int)  : Damage(amount)
    class Poison(amount: Int)   : Damage(amount)

    val typeName: String
        get() = when (this) {
            is Physical -> "физического"
            is Magical  -> "магического"
            is Poison   -> "ядовитого"
        }
}


enum class ItemType { POTION, SCROLL, WEAPON, RING }

data class Item(
    val name: String,
    val type: ItemType,
    val effect: Int,
)


interface Defendable {
    fun block(): Int
}

interface Castable {
    fun castSpell(target: Character)
}


abstract class Character(
    val name: String,
    maxHealth: Int,
) {
    var maxHealth: Int = maxHealth
        protected set

    var health: Int = maxHealth
        private set

    protected fun setHealth(value: Int) {
        health = value.coerceAtLeast(0)
    }

    open fun takeDamage(damage: Damage): Int {
        val dealt = damage.amount.coerceAtMost(health)
        setHealth(health - damage.amount)
        return dealt
    }

    val isAlive: Boolean
        get() = health > 0

    override fun toString(): String = "$name ($health/$maxHealth HP)"

    private val _inventory = mutableListOf<Item>()
    val inventory: List<Item> get() = _inventory.toList()

    fun addItem(item: Item) {
        _inventory.add(item)
    }

    open fun useItem(index: Int) {
        require(index in _inventory.indices) { "Нет предмета с индексом $index" }
        val item = _inventory[index]
        when (item.type) {
            ItemType.POTION -> {
                heal(item.effect)
                println("$name выпивает ${item.name} и восстанавливает ${item.effect} HP.")
                _inventory.removeAt(index)
            }
            ItemType.WEAPON -> {
                this.weaponBonus = item.effect
                println("$name экипирует ${item.name} (+${item.effect} к атаке).")
            }
            ItemType.SCROLL, ItemType.RING -> {
                println("$name использует ${item.name}, но эффект пока не реализован.")
            }
        }
    }

    fun showInventory() {
        if (_inventory.isEmpty()) {
            println("$name: инвентарь пуст.")
            return
        }
        println("Инвентарь $name:")
        _inventory.forEachIndexed { i, item ->
            println("  [$i] $item")
        }
    }

    var weaponBonus: Int = 0
        protected set

    protected fun heal(amount: Int) {
        setHealth(health + amount)
    }

    abstract fun attack(target: Character)
}


class Warrior(
    name: String,
    maxHealth: Int = 120,
    private val armor: Int = 10,
) : Character(name, maxHealth), Defendable {

    override fun block(): Int = armor

    override fun takeDamage(damage: Damage): Int {
        val actual = when (damage) {
            is Damage.Physical -> (damage.amount - armor).coerceAtLeast(0)
            is Damage.Magical, is Damage.Poison -> damage.amount
        }
        val dealt = actual.coerceAtMost(health)
        setHealth(health - actual)
        return dealt
    }

    override fun attack(target: Character) {
        val dmg = Damage.Physical(15 + weaponBonus)
        val dealt = target.takeDamage(dmg)
        println("$name наносит $dealt ${dmg.typeName} урона ${target.name}. У ${target.name} осталось ${target.health} HP.")
    }
}

class Goblin(
    name: String,
    maxHealth: Int = 60,
) : Character(name, maxHealth) {

    override fun takeDamage(damage: Damage): Int {
        val actual = when (damage) {
            is Damage.Magical -> damage.amount / 2
            is Damage.Physical, is Damage.Poison -> damage.amount
        }
        val dealt = actual.coerceAtMost(health)
        setHealth(health - actual)
        return dealt
    }

    override fun attack(target: Character) {
        val dmg = Damage.Physical(8 + weaponBonus)
        val dealt = target.takeDamage(dmg)
        println("$name кусает ${target.name} и наносит $dealt ${dmg.typeName} урона. У ${target.name} осталось ${target.health} HP.")
    }
}

class Mage(
    name: String,
    maxHealth: Int = 70,
    private val magicPower: Int = 25,
) : Character(name, maxHealth), Castable {

    override fun takeDamage(damage: Damage): Int {
        val actual = when (damage) {
            is Damage.Physical -> (damage.amount * 6 + 4) / 5
            is Damage.Magical, is Damage.Poison -> damage.amount
        }
        val dealt = actual.coerceAtMost(health)
        setHealth(health - actual)
        return dealt
    }

    override fun castSpell(target: Character) {
        val dmg = Damage.Magical(magicPower)
        val dealt = target.takeDamage(dmg)
        println("$name швыряет сгусток пламени в ${target.name} и наносит $dealt ${dmg.typeName} урона. У ${target.name} осталось ${target.health} HP.")
    }

    override fun attack(target: Character) = castSpell(target)
}

class Arena(
    private val fighterA: Character,
    private val fighterB: Character,
) {
    fun fight() {
        println("⚔  ${fighterA.name} vs ${fighterB.name} — бой начинается!")
        var turn = 0
        while (fighterA.isAlive && fighterB.isAlive) {
            val attacker = if (turn % 2 == 0) fighterA else fighterB
            val defender = if (turn % 2 == 0) fighterB else fighterA

            attacker.attack(defender)

            if (!defender.isAlive) {
                println("${defender.name} пал в бою!")
                println("Победитель: ${attacker.name}")
                return
            }
            turn++
        }
    }
}

fun main() {
    println("  БОЙ 1: Воин vs Гоблин  ")
    val warrior = Warrior("Артур")
    val goblin  = Goblin("Гоблин-разбойник")

    warrior.addItem(Item("Стальной меч", ItemType.WEAPON, 5))
    warrior.addItem(Item("Зелье лечения", ItemType.POTION, 30))
    warrior.showInventory()

    Arena(warrior, goblin).fight()

    println()
    println(" БОЙ 2: Маг vs Воин  ")
    val mage = Mage("Мерлин", magicPower = 20)
    val knight = Warrior("Ланселот", maxHealth = 100, armor = 8)

    Arena(mage, knight).fight()
}