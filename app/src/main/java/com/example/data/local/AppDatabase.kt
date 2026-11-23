package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        FamilyMemberEntity::class,
        ShoppingItemEntity::class,
        CalendarEventEntity::class,
        FamilyTaskEntity::class,
        MealPlanEntity::class,
        RecipeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun familyDao(): FamilyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "family_wall_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.familyDao())
                }
            }
        }

        private suspend fun populateInitialData(dao: FamilyDao) {
            // Family Members
            val members = listOf(
                FamilyMemberEntity(name = "Mamá", role = "Mamá (Laura)", avatarEmoji = "👩", colorHex = "#EC4899"),
                FamilyMemberEntity(name = "Papá", role = "Papá (Carlos)", avatarEmoji = "👨", colorHex = "#3B82F6"),
                FamilyMemberEntity(name = "Sofía", role = "Hija (12 años)", avatarEmoji = "👧", colorHex = "#8B5CF6"),
                FamilyMemberEntity(name = "Lucas", role = "Hijo (8 años)", avatarEmoji = "👦", colorHex = "#10B981")
            )
            dao.insertMembers(members)

            // Shopping Items (Mercadona, Carrefour, Lidl, Alcampo with Urgente, Club and normal options)
            val items = listOf(
                ShoppingItemEntity(
                    name = "Leche entera fresca",
                    quantity = 6.0,
                    unit = "L",
                    supermarket = "Mercadona",
                    priority = "URGENTE",
                    hasOffer = false,
                    offerDetails = "",
                    category = "Lácteos",
                    isChecked = false,
                    addedByMemberName = "Mamá",
                    notes = "De la nevera, no uht"
                ),
                ShoppingItemEntity(
                    name = "Yogures estilo griego natural",
                    quantity = 4.0,
                    unit = "packs",
                    supermarket = "Lidl",
                    priority = "CLUB",
                    hasOffer = true,
                    offerDetails = "Cupón Lidl Plus -25%",
                    category = "Lácteos",
                    isChecked = false,
                    addedByMemberName = "Papá",
                    notes = "Activar cupón en la app"
                ),
                ShoppingItemEntity(
                    name = "Detergente líquido lavadora",
                    quantity = 1.0,
                    unit = "ud",
                    supermarket = "Mercadona",
                    priority = "URGENTE",
                    hasOffer = false,
                    offerDetails = "",
                    category = "Limpieza",
                    isChecked = false,
                    addedByMemberName = "Mamá",
                    notes = "Quedan solo 2 lavados"
                ),
                ShoppingItemEntity(
                    name = "Papel higiénico acolchado",
                    quantity = 2.0,
                    unit = "packs",
                    supermarket = "Carrefour",
                    priority = "CLUB",
                    hasOffer = true,
                    offerDetails = "3x2 con Tarjeta Pass",
                    category = "Limpieza",
                    isChecked = false,
                    addedByMemberName = "Papá",
                    notes = "Pasar tarjeta del Club"
                ),
                ShoppingItemEntity(
                    name = "Filetes de Salmón noruego",
                    quantity = 600.0,
                    unit = "g",
                    supermarket = "Mercadona",
                    priority = "URGENTE",
                    hasOffer = false,
                    offerDetails = "",
                    category = "Pescadería",
                    isChecked = false,
                    addedByMemberName = "Mamá",
                    notes = "Para la cena de hoy"
                ),
                ShoppingItemEntity(
                    name = "Aceite de Oliva Virgen Extra",
                    quantity = 3.0,
                    unit = "L",
                    supermarket = "Alcampo",
                    priority = "NORMAL",
                    hasOffer = true,
                    offerDetails = "Oferta folleto 6.75€/L",
                    category = "Despensa",
                    isChecked = false,
                    addedByMemberName = "Papá",
                    notes = "Garrafa o botella cristal"
                ),
                ShoppingItemEntity(
                    name = "Plátanos de Canarias",
                    quantity = 1.5,
                    unit = "kg",
                    supermarket = "Mercadona",
                    priority = "NORMAL",
                    hasOffer = false,
                    offerDetails = "",
                    category = "Frutas y Verduras",
                    isChecked = false,
                    addedByMemberName = "Lucas",
                    notes = "Que no estén muy maduros"
                ),
                ShoppingItemEntity(
                    name = "Cápsulas de café compatibles",
                    quantity = 2.0,
                    unit = "packs",
                    supermarket = "Carrefour",
                    priority = "CLUB",
                    hasOffer = true,
                    offerDetails = "Segunda unidad al 50%",
                    category = "Despensa",
                    isChecked = false,
                    addedByMemberName = "Mamá",
                    notes = "Intensidad 9 o superior"
                ),
                ShoppingItemEntity(
                    name = "Pan rústico hogaza",
                    quantity = 1.0,
                    unit = "ud",
                    supermarket = "Lidl",
                    priority = "NORMAL",
                    hasOffer = false,
                    offerDetails = "",
                    category = "Panadería",
                    isChecked = true,
                    addedByMemberName = "Sofía",
                    notes = "Recién horneado"
                )
            )
            dao.insertShoppingItems(items)

            // Calendar Events
            val cal = Calendar.getInstance()
            val todayMillis = cal.timeInMillis

            cal.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrowMillis = cal.timeInMillis

            cal.add(Calendar.DAY_OF_YEAR, 2)
            val inThreeDaysMillis = cal.timeInMillis

            val events = listOf(
                CalendarEventEntity(
                    title = "Tutoría escolar de Sofía",
                    description = "Reunión con el tutor sobre el curso y notas",
                    location = "Colegio San José - Aula 2B",
                    dateEpochMillis = todayMillis,
                    startTime = "16:30",
                    endTime = "17:15",
                    assignedMemberName = "Mamá",
                    memberColorHex = "#EC4899",
                    category = "ESCUELA",
                    reminderMinutesBefore = 30
                ),
                CalendarEventEntity(
                    title = "Entrenamiento fútbol de Lucas",
                    description = "Llevar botas de tacos y botella de agua",
                    location = "Polideportivo Municipal Campo 1",
                    dateEpochMillis = todayMillis,
                    startTime = "17:45",
                    endTime = "19:00",
                    assignedMemberName = "Papá",
                    memberColorHex = "#3B82F6",
                    category = "EXTRAESCOLAR",
                    reminderMinutesBefore = 15
                ),
                CalendarEventEntity(
                    title = "Dentista - Revisión semestral",
                    description = "Revisión y limpieza para los niños",
                    location = "Clínica Dentalia - Gran Vía 14",
                    dateEpochMillis = tomorrowMillis,
                    startTime = "11:30",
                    endTime = "12:30",
                    assignedMemberName = "Todos",
                    memberColorHex = "#EF4444",
                    category = "SALUD",
                    reminderMinutesBefore = 60
                ),
                CalendarEventEntity(
                    title = "Noche de Pizza y Película Familiar",
                    description = "Hacer la masa juntos y ver película animada",
                    location = "Salón de casa",
                    dateEpochMillis = inThreeDaysMillis,
                    startTime = "20:30",
                    endTime = "22:30",
                    assignedMemberName = "Todos",
                    memberColorHex = "#F59E0B",
                    category = "OCIO",
                    reminderMinutesBefore = 15
                )
            )
            dao.insertEvents(events)

            // Tasks
            val tasks = listOf(
                FamilyTaskEntity(
                    title = "Comprar cartulinas y pegamento para plástica",
                    description = "Sofía necesita para la maqueta de ciencias",
                    assignedMemberName = "Sofía",
                    assignedMemberEmoji = "👧",
                    dueDate = "Hoy",
                    priority = "ALTA",
                    isCompleted = false,
                    category = "Colegio"
                ),
                FamilyTaskEntity(
                    title = "Poner lavadora de ropa blanca y tender",
                    description = "Poner programa eco con suavizante",
                    assignedMemberName = "Mamá",
                    assignedMemberEmoji = "👩",
                    dueDate = "Hoy",
                    priority = "MEDIA",
                    isCompleted = false,
                    category = "Hogar"
                ),
                FamilyTaskEntity(
                    title = "Revisar presión de neumáticos del coche",
                    description = "Antes del viaje del fin de semana",
                    assignedMemberName = "Papá",
                    assignedMemberEmoji = "👨",
                    dueDate = "Mañana",
                    priority = "MEDIA",
                    isCompleted = false,
                    category = "Coche"
                ),
                FamilyTaskEntity(
                    title = "Bajar la basura orgánica y reciclar plástico",
                    description = "Antes de cenar",
                    assignedMemberName = "Lucas",
                    assignedMemberEmoji = "👦",
                    dueDate = "Hoy",
                    priority = "MEDIA",
                    isCompleted = false,
                    category = "Hogar"
                ),
                FamilyTaskEntity(
                    title = "Preparar mochila para natación",
                    description = "Gorro, bañador, gafas y toalla",
                    assignedMemberName = "Lucas",
                    assignedMemberEmoji = "👦",
                    dueDate = "Viernes",
                    priority = "BAJA",
                    isCompleted = false,
                    category = "Deporte"
                ),
                FamilyTaskEntity(
                    title = "Llamar a la abuela por su santo",
                    description = "Felicitarla toda la familia",
                    assignedMemberName = "Todos",
                    assignedMemberEmoji = "⭐",
                    dueDate = "Jueves",
                    priority = "ALTA",
                    isCompleted = true,
                    category = "Familia"
                )
            )
            dao.insertTasks(tasks)

            // Meal Plans for week (Lunes a Domingo)
            val meals = listOf(
                MealPlanEntity(dayOfWeekIndex = 0, mealType = "DESAYUNO", dishName = "Tostadas con tomate y aceite"),
                MealPlanEntity(dayOfWeekIndex = 0, mealType = "COMIDA", dishName = "Lentejas estofadas con verduras"),
                MealPlanEntity(dayOfWeekIndex = 0, mealType = "MERIENDA", dishName = "Fruta de temporada y yogur"),
                MealPlanEntity(dayOfWeekIndex = 0, mealType = "CENA", dishName = "Tortilla francesa con ensalada verde"),

                MealPlanEntity(dayOfWeekIndex = 1, mealType = "DESAYUNO", dishName = "Avena con plátano y canela"),
                MealPlanEntity(dayOfWeekIndex = 1, mealType = "COMIDA", dishName = "Pechuga de pollo al limón con arroz"),
                MealPlanEntity(dayOfWeekIndex = 1, mealType = "MERIENDA", dishName = "Bocadillo de jamón york"),
                MealPlanEntity(dayOfWeekIndex = 1, mealType = "CENA", dishName = "Crema suave de calabacín"),

                MealPlanEntity(dayOfWeekIndex = 2, mealType = "DESAYUNO", dishName = "Zumo natural y galletas caseras"),
                MealPlanEntity(dayOfWeekIndex = 2, mealType = "COMIDA", dishName = "Macarrones boloñesa tradicional"),
                MealPlanEntity(dayOfWeekIndex = 2, mealType = "MERIENDA", dishName = "Yogur griego con frutos secos"),
                MealPlanEntity(dayOfWeekIndex = 2, mealType = "CENA", dishName = "Salmón a la plancha con espárragos"),

                MealPlanEntity(dayOfWeekIndex = 3, mealType = "DESAYUNO", dishName = "Tostadas con aguacate y queso"),
                MealPlanEntity(dayOfWeekIndex = 3, mealType = "COMIDA", dishName = "Arroz con verduras y ternera"),
                MealPlanEntity(dayOfWeekIndex = 3, mealType = "MERIENDA", dishName = "Manzana y nueces"),
                MealPlanEntity(dayOfWeekIndex = 3, mealType = "CENA", dishName = "Sopa de fideos y sándwich mixto"),

                MealPlanEntity(dayOfWeekIndex = 4, mealType = "DESAYUNO", dishName = "Café / Colacao y bizcocho casero"),
                MealPlanEntity(dayOfWeekIndex = 4, mealType = "COMIDA", dishName = "Merluza al horno con patatas panaderas"),
                MealPlanEntity(dayOfWeekIndex = 4, mealType = "MERIENDA", dishName = "Batido de fresa casero"),
                MealPlanEntity(dayOfWeekIndex = 4, mealType = "CENA", dishName = "¡Pizza casera crujiente familiar!"),

                MealPlanEntity(dayOfWeekIndex = 5, mealType = "COMIDA", dishName = "Paella de pollo y verduras"),
                MealPlanEntity(dayOfWeekIndex = 5, mealType = "CENA", dishName = "Hamburguesas gourmet caseras"),

                MealPlanEntity(dayOfWeekIndex = 6, mealType = "COMIDA", dishName = "Pollo asado al romero con patatas"),
                MealPlanEntity(dayOfWeekIndex = 6, mealType = "CENA", dishName = "Ensalada campera ligera")
            )
            dao.insertMealPlans(meals)

            // Recipes
            val recipes = listOf(
                RecipeEntity(
                    title = "Pizza Casera Crujiente",
                    category = "Cenas Rápidas",
                    prepTimeMinutes = 40,
                    servings = 4,
                    ingredients = "400g Harina de trigo\n250ml Agua tibia\n7g Levadura de panadería\n1 cucharada Aceite de oliva\n200g Tomate frito triturado\n250g Queso mozzarella rallado\n150g Jamón cocido\nOrégano seco",
                    instructions = "1. Mezclar harina, levadura, agua tibia y sal hasta formar masa elástica.\n2. Dejar reposar 20 min cubierta con trapo.\n3. Estirar fino en bandeja con papel de horno.\n4. Untar tomate, mozzarella y orégano.\n5. Hornear a 220°C durante 12-15 min hasta que los bordes doren.",
                    emoji = "🍕",
                    isFavorite = true
                ),
                RecipeEntity(
                    title = "Lentejas Estofadas Tradicionales",
                    category = "Guisos",
                    prepTimeMinutes = 50,
                    servings = 4,
                    ingredients = "350g Lentejas pardinas\n1 Cebolla picada\n2 Zanahorias en rodajas\n1 Pimiento verde\n1 Patata en dados\n1 cucharadita Pimentón dulce de la Vera\n1 Hoja de laurel\n3 cucharadas Aceite de oliva\nSal al gusto",
                    instructions = "1. Poner las lentejas enjuagadas en olla con agua fría.\n2. Añadir verduras troceadas, laurel, pimentón y chorrito de aceite.\n3. Llevar a ebullición y cocinar a fuego lento durante 45 minutos.\n4. Rectificar de sal y dejar reposar 5 min antes de servir.",
                    emoji = "🍲",
                    isFavorite = true
                ),
                RecipeEntity(
                    title = "Crema Suave de Calabacín y Quesitos",
                    category = "Primeros Platos",
                    prepTimeMinutes = 25,
                    servings = 4,
                    ingredients = "3 Calabacines medianos lavados\n1 Puerro limpio\n1 Patata pequeña\n4 Quesitos suaves en porciones\n2 cucharadas Aceite de oliva virgen extra\nPizca de sal y pimienta blanca",
                    instructions = "1. Cortar puerro, calabacines y patata en rodajas.\n2. Rehogar con aceite 5 min en la cazuela.\n3. Cubrir con agua justa y cocer 15 min.\n4. Añadir los quesitos y triturar hasta consistencia sedosa.",
                    emoji = "🥣",
                    isFavorite = false
                ),
                RecipeEntity(
                    title = "Macarrones Boloñesa Casera",
                    category = "Pastas",
                    prepTimeMinutes = 35,
                    servings = 4,
                    ingredients = "400g Macarrones pluma\n300g Carne picada mixta\n1 Cebolla dulce\n1 Zanahoria rallada\n400g Salsa de tomate casera\nQueso parmesano para espolvorear\nSal, orégano y albahaca",
                    instructions = "1. Sofreír la cebolla y zanahoria con aceite.\n2. Añadir la carne picada y dorar bien.\n3. Agregar el tomate y hierbas aromáticas; cocinar a fuego suave 15 min.\n4. Cocer la pasta al dente, mezclar con la salsa y coronar con queso.",
                    emoji = "🍝",
                    isFavorite = true
                )
            )
            dao.insertRecipes(recipes)
        }
    }
}
