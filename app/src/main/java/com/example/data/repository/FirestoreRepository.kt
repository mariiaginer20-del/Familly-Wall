package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CalendarEvent
import com.example.data.model.CategoryScope
import com.example.data.model.CustomList
import com.example.data.model.CustomListItem
import com.example.data.model.DefaultCategories
import com.example.data.model.DefaultMacroIngredients
import com.example.data.model.FamilyCategory
import com.example.data.model.FamilyMember
import com.example.data.model.FamilyTask
import com.example.data.model.FamilyUnit
import com.example.data.model.MacroIngredient
import com.example.data.model.MacroTargets
import com.example.data.model.MealPlan
import com.example.data.model.PetFeedingStatus
import com.example.data.model.ProductHistoryEntry
import com.example.data.model.Recipe
import com.example.data.model.RecipeFormatters
import com.example.data.model.ShoppingItem
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

class FirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val random = SecureRandom()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión con Google para continuar.")
    }

    private fun <T> observeCollection(
        familyId: String,
        subcollection: String,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || familyId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "families/$familyId/$subcollection"
        emitAll(
            db.collection("families")
                .document(familyId)
                .collection(subcollection)
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(clazz, ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // --- USER PROFILE & FAMILY UNIT ---

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        if (uid.isBlank() || auth.currentUser?.uid != uid) {
            emit(null)
            return@flow
        }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeFamilyUnit(familyId: String): Flow<FamilyUnit?> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || familyId.isBlank()) {
            emit(null)
            return@flow
        }
        val path = "families/$familyId"
        emitAll(
            db.collection("families").document(familyId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(FamilyUnit::class.java, ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    private fun generateInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(6) {
            repeat(6) {
                append(alphabet[random.nextInt(alphabet.length)])
            }
        }
    }

    suspend fun createFamilyUnit(
        familyName: String,
        memberName: String,
        avatarEmoji: String,
        colorHex: String,
        seedStarterData: Boolean = true
    ): Result<String> {
        var currentPath = "families"
        return try {
            val uid = requireUserId()
            val cleanFamily = familyName.trim().take(60)
            val cleanMember = memberName.trim().take(40)
            require(cleanFamily.isNotBlank()) { "El nombre del hogar no puede estar vacío." }
            require(cleanMember.isNotBlank()) { "Tu nombre no puede estar vacío." }

            var inviteCode = generateInviteCode()
            for (attempt in 0 until 3) {
                val snap = db.collection("inviteCodes").document(inviteCode).get().await()
                if (!snap.exists()) break
                inviteCode = generateInviteCode()
            }

            val familyRef = db.collection("families").document()
            val familyId = familyRef.id
            currentPath = familyRef.path
            val inviteRef = db.collection("inviteCodes").document(inviteCode)
            val memberRef = familyRef.collection("members").document(uid)
            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()

            val batch = db.batch()
            batch.set(
                familyRef,
                mapOf(
                    "name" to cleanFamily,
                    "inviteCode" to inviteCode,
                    "ownerUid" to uid,
                    "memberUids" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                inviteRef,
                mapOf(
                    "familyId" to familyId,
                    "familyName" to cleanFamily,
                    "ownerUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                memberRef,
                mapOf(
                    "familyId" to familyId,
                    "linkedUid" to uid,
                    "name" to cleanMember,
                    "role" to "Adulto",
                    "avatarEmoji" to avatarEmoji,
                    "colorHex" to colorHex,
                    "memberUids" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (userSnap.exists()) {
                batch.update(
                    userRef,
                    mapOf(
                        "displayName" to cleanMember,
                        "familyId" to familyId,
                        "avatarEmoji" to avatarEmoji,
                        "colorHex" to colorHex,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                val userPayload = mutableMapOf<String, Any>(
                    "uid" to uid,
                    "displayName" to cleanMember,
                    "familyId" to familyId,
                    "avatarEmoji" to avatarEmoji,
                    "colorHex" to colorHex,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                auth.currentUser?.email?.takeIf { it.isNotBlank() && it.length <= 150 }?.let {
                    userPayload["email"] = it
                }
                batch.set(userRef, userPayload)
            }

            if (seedStarterData) {
                val shopRef = familyRef.collection("shoppingItems").document()
                batch.set(
                    shopRef,
                    mapOf(
                        "familyId" to familyId,
                        "name" to "Leche entera",
                        "quantity" to 2.0,
                        "unit" to "L",
                        "supermarket" to "Mercadona",
                        "priority" to "NORMAL",
                        "hasOffer" to false,
                        "offerDetails" to "",
                        "category" to "Lácteos",
                        "checked" to false,
                        "addedByMemberName" to cleanMember,
                        "notes" to "",
                        "createdByUid" to uid,
                        "memberUids" to listOf(uid),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val taskRef = familyRef.collection("tasks").document()
                batch.set(
                    taskRef,
                    mapOf(
                        "familyId" to familyId,
                        "title" to "Compartir código familiar ($inviteCode)",
                        "description" to "Pasa el código a tu pareja para sincronizar todo en tiempo real.",
                        "assignedMemberName" to cleanMember,
                        "assignedMemberEmoji" to avatarEmoji,
                        "dueDate" to "Hoy",
                        "priority" to "ALTA",
                        "completed" to false,
                        "category" to "Hogar",
                        "createdByUid" to uid,
                        "memberUids" to listOf(uid),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val recipeRef = familyRef.collection("recipes").document()
                batch.set(
                    recipeRef,
                    mapOf(
                        "familyId" to familyId,
                        "title" to "Tortilla de patatas",
                        "category" to "Cenas Rápidas",
                        "prepTimeMinutes" to 25,
                        "servings" to 4,
                        "ingredients" to "4 patatas medianas\n5 huevos\n1 cebolla\nAceite de oliva\nSal",
                        "instructions" to "1. Pelar y cortar las patatas.\n2. Pochar despacio.\n3. Batir los huevos y cuajar en sartén.",
                        "emoji" to "🍳",
                        "favorite" to true,
                        "createdByUid" to uid,
                        "memberUids" to listOf(uid),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(familyId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, currentPath)
            Result.failure(e)
        }
    }

    suspend fun joinFamilyByCode(
        rawCode: String,
        memberName: String,
        avatarEmoji: String,
        colorHex: String
    ): Result<String> {
        val code = rawCode.trim().uppercase()
        var currentPath = "inviteCodes/$code"
        return try {
            val uid = requireUserId()
            val cleanMember = memberName.trim().take(40)
            require(code.matches(Regex("^[A-Z0-9]{4,12}$"))) {
                "El código debe tener entre 4 y 12 letras o números."
            }
            require(cleanMember.isNotBlank()) { "Tu nombre no puede estar vacío." }

            val inviteSnap = db.collection("inviteCodes").document(code).get().await()
            if (!inviteSnap.exists()) {
                return Result.failure(IllegalArgumentException("No existe ninguna unidad familiar con ese código."))
            }
            val familyId = inviteSnap.getString("familyId")
                ?: return Result.failure(IllegalStateException("Código familiar inválido."))

            val familyRef = db.collection("families").document(familyId)
            val userRef = db.collection("users").document(uid)
            currentPath = userRef.path
            val userSnap = userRef.get().await()

            currentPath = "${familyRef.path} + ${userRef.path}"
            val batch = db.batch()
            batch.update(
                familyRef,
                mapOf(
                    "memberUids" to FieldValue.arrayUnion(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (userSnap.exists() &&
                !userSnap.getString("uid").isNullOrBlank() &&
                userSnap.getTimestamp("createdAt") != null
            ) {
                val updateMap = mutableMapOf<String, Any>(
                    "displayName" to cleanMember,
                    "familyId" to familyId,
                    "avatarEmoji" to avatarEmoji,
                    "colorHex" to colorHex,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                auth.currentUser?.email?.takeIf { it.isNotBlank() && it.length <= 150 }?.let {
                    updateMap["email"] = it
                }
                batch.update(userRef, updateMap)
            } else {
                val userPayload = mutableMapOf<String, Any>(
                    "uid" to uid,
                    "displayName" to cleanMember,
                    "familyId" to familyId,
                    "avatarEmoji" to avatarEmoji,
                    "colorHex" to colorHex,
                    "createdAt" to (userSnap.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                auth.currentUser?.email?.takeIf { it.isNotBlank() && it.length <= 150 }?.let {
                    userPayload["email"] = it
                }
                batch.set(userRef, userPayload)
            }
            batch.commit().await()

            // Now signed-in user is in families/{familyId}.memberUids; read full memberUids and register member card
            currentPath = familyRef.path
            val updatedFamilySnap = familyRef.get().await()
            @Suppress("UNCHECKED_CAST")
            val memberUids = (updatedFamilySnap.get("memberUids") as? List<String>)?.take(15) ?: listOf(uid)

            val memberRef = familyRef.collection("members").document(uid)
            currentPath = memberRef.path
            val existingMember = memberRef.get().await()
            if (existingMember.exists()) {
                memberRef.update(
                    mapOf(
                        "name" to cleanMember,
                        "avatarEmoji" to avatarEmoji,
                        "colorHex" to colorHex,
                        "memberUids" to memberUids,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                memberRef.set(
                    mapOf(
                        "familyId" to familyId,
                        "linkedUid" to uid,
                        "name" to cleanMember,
                        "role" to "Adulto",
                        "avatarEmoji" to avatarEmoji,
                        "colorHex" to colorHex,
                        "memberUids" to memberUids,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(familyId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            val detailed = if (e is FirebaseFirestoreException &&
                e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            ) {
                IllegalStateException("${e.localizedMessage ?: "PERMISSION_DENIED"} ($currentPath)", e)
            } else {
                e
            }
            Result.failure(detailed)
        }
    }

    suspend fun leaveFamily(family: FamilyUnit): Result<Unit> {
        val currentPath = "families/${family.id}"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            val batch = db.batch()
            if (family.ownerUid != uid && uid in family.memberUids) {
                batch.update(
                    db.collection("families").document(family.id),
                    mapOf(
                        "memberUids" to FieldValue.arrayRemove(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.update(
                userRef,
                mapOf(
                    "familyId" to FieldValue.delete(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, currentPath)
            Result.failure(e)
        }
    }

    /**
     * Propagates updated family.memberUids to any subcollection documents that do not yet include
     * newly joined partners, keeping flat allow-list queries synchronized across devices.
     */
    suspend fun syncMemberUidsAcrossFamily(family: FamilyUnit) {
        val uid = auth.currentUser?.uid ?: return
        if (family.id.isBlank() || family.memberUids.isEmpty() || uid !in family.memberUids) return
        val targetSet = family.memberUids.toSet()
        val subcollections = listOf(
            "members",
            "shoppingItems",
            "customLists",
            "customListItems",
            "calendarEvents",
            "tasks",
            "mealPlans",
            "recipes",
            "categories",
            "productHistory",
            "petCare",
            "macroTargets",
            "macroIngredients"
        )
        try {
            val refsToUpdate = mutableListOf<DocumentReference>()
            for (col in subcollections) {
                val snap = db.collection("families")
                    .document(family.id)
                    .collection(col)
                    .whereArrayContains("memberUids", uid)
                    .get()
                    .await()
                for (doc in snap.documents) {
                    @Suppress("UNCHECKED_CAST")
                    val docUids = (doc.get("memberUids") as? List<String>)?.toSet() ?: emptySet()
                    if (docUids != targetSet) {
                        refsToUpdate.add(doc.reference)
                    }
                }
            }
            refsToUpdate.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { ref ->
                    batch.update(
                        ref,
                        mapOf(
                            "memberUids" to family.memberUids,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "families/${family.id}")
        }
    }

    // --- MEMBERS ---

    fun observeMembers(familyId: String): Flow<List<FamilyMember>> =
        observeCollection(familyId, "members", FamilyMember::class.java)

    suspend fun addFamilyMember(
        familyId: String,
        memberUids: List<String>,
        name: String,
        role: String,
        avatarEmoji: String,
        colorHex: String
    ): Result<Unit> {
        val path = "families/$familyId/members"
        return try {
            requireUserId()
            val docRef = db.collection("families").document(familyId).collection("members").document()
            docRef.set(
                mapOf(
                    "familyId" to familyId,
                    "name" to name.trim().take(40),
                    "role" to role.trim().take(30),
                    "avatarEmoji" to avatarEmoji,
                    "colorHex" to colorHex,
                    "memberUids" to memberUids,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- SHOPPING ITEMS ---

    fun observeShoppingItems(familyId: String): Flow<List<ShoppingItem>> =
        observeCollection(familyId, "shoppingItems", ShoppingItem::class.java)

    suspend fun addShoppingItem(
        familyId: String,
        memberUids: List<String>,
        item: ShoppingItem
    ): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("shoppingItems").document()
        return try {
            val uid = requireUserId()
            docRef.set(
                mapOf(
                    "familyId" to familyId,
                    "name" to item.name.trim().take(120),
                    "quantity" to item.quantity.coerceIn(0.1, 10000.0),
                    "unit" to item.unit.trim().take(20),
                    "supermarket" to item.supermarket.trim().take(40),
                    "priority" to item.priority,
                    "hasOffer" to item.hasOffer,
                    "offerDetails" to item.offerDetails.trim().take(150),
                    "category" to item.category.trim().take(60),
                    "checked" to false,
                    "addedByMemberName" to item.addedByMemberName.trim().take(40),
                    "assignedMemberName" to item.assignedMemberName.trim().ifBlank { "Todos" }.take(40),
                    "notes" to item.notes.trim().take(300),
                    "createdByUid" to uid,
                    "memberUids" to memberUids,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateShoppingItem(familyId: String, item: ShoppingItem): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("shoppingItems").document(item.id)
        return try {
            requireUserId()
            require(item.id.isNotBlank())
            docRef.update(
                mapOf(
                    "name" to item.name.trim().take(120),
                    "quantity" to item.quantity.coerceIn(0.1, 10000.0),
                    "unit" to item.unit.trim().take(20),
                    "supermarket" to item.supermarket.trim().take(40),
                    "priority" to item.priority,
                    "hasOffer" to item.hasOffer,
                    "offerDetails" to item.offerDetails.trim().take(150),
                    "category" to item.category.trim().take(60),
                    "checked" to item.checked,
                    "addedByMemberName" to item.addedByMemberName.trim().take(40),
                    "assignedMemberName" to item.assignedMemberName.trim().ifBlank { "Todos" }.take(40),
                    "notes" to item.notes.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun setShoppingItemChecked(familyId: String, itemId: String, checked: Boolean): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("shoppingItems").document(itemId)
        return try {
            requireUserId()
            docRef.update(mapOf("checked" to checked, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateShoppingItemQuantity(familyId: String, itemId: String, quantity: Double): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("shoppingItems").document(itemId)
        return try {
            requireUserId()
            docRef.update(
                mapOf(
                    "quantity" to quantity.coerceIn(0.1, 10000.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteShoppingItem(familyId: String, itemId: String): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("shoppingItems").document(itemId)
        return try {
            requireUserId()
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCheckedShoppingItems(familyId: String, items: List<ShoppingItem>): Result<Unit> {
        val checkedItems = items.filter { it.checked && it.id.isNotBlank() }
        if (checkedItems.isEmpty()) return Result.success(Unit)
        return try {
            requireUserId()
            checkedItems.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { item ->
                    batch.delete(
                        db.collection("families").document(familyId).collection("shoppingItems").document(item.id)
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "families/$familyId/shoppingItems")
            Result.failure(e)
        }
    }

    suspend fun addSelectedIngredientsToShoppingList(
        familyId: String,
        memberUids: List<String>,
        recipeTitle: String,
        ingredients: List<String>,
        supermarket: String,
        addedByMemberName: String
    ): Result<Unit> {
        val cleaned = ingredients.map { it.trim() }.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) return Result.success(Unit)
        return try {
            val uid = requireUserId()
            cleaned.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { line ->
                    val (parsedName, parsedQty, parsedUnit) = RecipeFormatters.parseIngredientForShopping(line)
                    val docRef = db.collection("families").document(familyId).collection("shoppingItems").document()
                    batch.set(
                        docRef,
                        mapOf(
                            "familyId" to familyId,
                            "name" to parsedName,
                            "quantity" to parsedQty,
                            "unit" to parsedUnit,
                            "supermarket" to supermarket.take(40),
                            "priority" to "NORMAL",
                            "hasOffer" to false,
                            "offerDetails" to "",
                            "category" to "Recetas ($recipeTitle)".take(60),
                            "checked" to false,
                            "addedByMemberName" to addedByMemberName.take(40),
                            "assignedMemberName" to "Todos",
                            "notes" to "Para: $recipeTitle".take(300),
                            "createdByUid" to uid,
                            "memberUids" to memberUids,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "families/$familyId/shoppingItems")
            Result.failure(e)
        }
    }

    // --- GENERAL CUSTOM LISTS ---

    fun observeCustomLists(familyId: String): Flow<List<CustomList>> =
        observeCollection(familyId, "customLists", CustomList::class.java)

    fun observeCustomListItems(familyId: String): Flow<List<CustomListItem>> =
        observeCollection(familyId, "customListItems", CustomListItem::class.java)

    suspend fun createCustomList(
        familyId: String,
        memberUids: List<String>,
        title: String,
        emoji: String,
        colorHex: String
    ): Result<String> {
        val docRef = db.collection("families").document(familyId).collection("customLists").document()
        return try {
            val uid = requireUserId()
            docRef.set(
                mapOf(
                    "familyId" to familyId,
                    "title" to title.trim().take(60),
                    "emoji" to emoji,
                    "colorHex" to colorHex,
                    "createdByUid" to uid,
                    "memberUids" to memberUids,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomListAndItems(
        familyId: String,
        listId: String,
        itemsInList: List<CustomListItem>
    ): Result<Unit> {
        val listRef = db.collection("families").document(familyId).collection("customLists").document(listId)
        return try {
            requireUserId()
            val batch = db.batch()
            itemsInList.filter { it.id.isNotBlank() }.forEach { item ->
                batch.delete(
                    db.collection("families").document(familyId).collection("customListItems").document(item.id)
                )
            }
            batch.delete(listRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, listRef.path)
            Result.failure(e)
        }
    }

    suspend fun addCustomListItem(
        familyId: String,
        memberUids: List<String>,
        item: CustomListItem
    ): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("customListItems").document()
        return try {
            val uid = requireUserId()
            docRef.set(
                mapOf(
                    "familyId" to familyId,
                    "listId" to item.listId,
                    "title" to item.title.trim().take(120),
                    "quantity" to item.quantity.trim().take(40),
                    "notes" to item.notes.trim().take(300),
                    "assignedMemberName" to item.assignedMemberName.trim().take(40),
                    "checked" to false,
                    "createdByUid" to uid,
                    "memberUids" to memberUids,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCustomListItem(familyId: String, item: CustomListItem): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("customListItems").document(item.id)
        return try {
            requireUserId()
            require(item.id.isNotBlank())
            docRef.update(
                mapOf(
                    "title" to item.title.trim().take(120),
                    "quantity" to item.quantity.trim().take(40),
                    "notes" to item.notes.trim().take(300),
                    "assignedMemberName" to item.assignedMemberName.trim().take(40),
                    "checked" to item.checked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun setCustomListItemChecked(familyId: String, itemId: String, checked: Boolean): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("customListItems").document(itemId)
        return try {
            requireUserId()
            docRef.update(mapOf("checked" to checked, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomListItem(familyId: String, itemId: String): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("customListItems").document(itemId)
        return try {
            requireUserId()
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- CALENDAR EVENTS ---

    fun observeEvents(familyId: String): Flow<List<CalendarEvent>> =
        observeCollection(familyId, "calendarEvents", CalendarEvent::class.java)

    suspend fun addEvent(
        familyId: String,
        memberUids: List<String>,
        event: CalendarEvent
    ): Result<CalendarEvent> {
        val docRef = db.collection("families").document(familyId).collection("calendarEvents").document()
        return try {
            val uid = requireUserId()
            val reminders = event.reminderMinutesList.distinct().take(10)
                .ifEmpty { listOf(event.reminderMinutesBefore.coerceIn(0, 10080)) }
            val primaryReminder = reminders.first().coerceIn(0, 10080)
            val cleanCategory = event.category.trim().ifBlank { "General" }.take(40)
            val extras = event.extraDatesEpochMillis.take(31)
            val endMillis = event.endDateEpochMillis.coerceAtLeast(0L)
            val sTime = if (event.allDay) "00:00" else event.startTime.trim().ifBlank { "09:00" }.take(10)
            val eTime = if (event.allDay) "23:59" else event.endTime.trim().ifBlank { "10:00" }.take(10)

            docRef.set(
                mapOf(
                    "familyId" to familyId,
                    "title" to event.title.trim().take(120),
                    "description" to event.description.trim().take(400),
                    "location" to event.location.trim().take(150),
                    "dateEpochMillis" to event.dateEpochMillis,
                    "endDateEpochMillis" to endMillis,
                    "extraDatesEpochMillis" to extras,
                    "allDay" to event.allDay,
                    "startTime" to sTime,
                    "endTime" to eTime,
                    "assignedMemberName" to event.assignedMemberName.trim().take(40),
                    "memberColorHex" to event.memberColorHex.trim().take(16),
                    "category" to cleanCategory,
                    "reminderMinutesBefore" to primaryReminder,
                    "reminderMinutesList" to reminders,
                    "notified" to false,
                    "createdByUid" to uid,
                    "memberUids" to memberUids,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(
                event.copy(
                    id = docRef.id,
                    familyId = familyId,
                    endDateEpochMillis = endMillis,
                    extraDatesEpochMillis = extras,
                    startTime = sTime,
                    endTime = eTime,
                    category = cleanCategory,
                    reminderMinutesBefore = primaryReminder,
                    reminderMinutesList = reminders,
                    createdByUid = uid,
                    memberUids = memberUids
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateEvent(familyId: String, event: CalendarEvent): Result<CalendarEvent> {
        val docRef = db.collection("families").document(familyId).collection("calendarEvents").document(event.id)
        return try {
            requireUserId()
            require(event.id.isNotBlank())
            val reminders = event.reminderMinutesList.distinct().take(10)
                .ifEmpty { listOf(event.reminderMinutesBefore.coerceIn(0, 10080)) }
            val primaryReminder = reminders.first().coerceIn(0, 10080)
            val cleanCategory = event.category.trim().ifBlank { "General" }.take(40)
            val extras = event.extraDatesEpochMillis.take(31)
            val endMillis = event.endDateEpochMillis.coerceAtLeast(0L)
            val sTime = if (event.allDay) "00:00" else event.startTime.trim().ifBlank { "09:00" }.take(10)
            val eTime = if (event.allDay) "23:59" else event.endTime.trim().ifBlank { "10:00" }.take(10)

            docRef.update(
                mapOf(
                    "title" to event.title.trim().take(120),
                    "description" to event.description.trim().take(400),
                    "location" to event.location.trim().take(150),
                    "dateEpochMillis" to event.dateEpochMillis,
                    "endDateEpochMillis" to endMillis,
                    "extraDatesEpochMillis" to extras,
                    "allDay" to event.allDay,
                    "startTime" to sTime,
                    "endTime" to eTime,
                    "assignedMemberName" to event.assignedMemberName.trim().take(40),
                    "memberColorHex" to event.memberColorHex.trim().take(16),
                    "category" to cleanCategory,
                    "reminderMinutesBefore" to primaryReminder,
                    "reminderMinutesList" to reminders,
                    "notified" to event.notified,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(
                event.copy(
                    endDateEpochMillis = endMillis,
                    extraDatesEpochMillis = extras,
                    startTime = sTime,
                    endTime = eTime,
                    category = cleanCategory,
                    reminderMinutesBefore = primaryReminder,
                    reminderMinutesList = reminders
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteEvent(familyId: String, eventId: String): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("calendarEvents").document(eventId)
        return try {
            requireUserId()
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- TASKS ---

    fun observeTasks(familyId: String): Flow<List<FamilyTask>> =
        observeCollection(familyId, "tasks", FamilyTask::class.java)

    suspend fun addTask(
        familyId: String,
        memberUids: List<String>,
        task: FamilyTask
    ): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("tasks").document()
        return try {
            val uid = requireUserId()
            docRef.set(
                mapOf(
                    "familyId" to familyId,
                    "title" to task.title.trim().take(120),
                    "description" to task.description.trim().take(400),
                    "assignedMemberName" to task.assignedMemberName.trim().take(40),
                    "assignedMemberEmoji" to task.assignedMemberEmoji,
                    "dueDate" to task.dueDate.trim().take(40),
                    "dueEpochMillis" to task.dueEpochMillis.coerceAtLeast(0L),
                    "dueTime" to task.dueTime.trim().take(10).ifBlank { "09:00" },
                    "reminderMinutesList" to task.reminderMinutesList.distinct().take(10),
                    "priority" to task.priority,
                    "completed" to false,
                    "category" to task.category.trim().take(40),
                    "createdByUid" to uid,
                    "memberUids" to memberUids,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTask(familyId: String, task: FamilyTask): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("tasks").document(task.id)
        return try {
            requireUserId()
            require(task.id.isNotBlank())
            docRef.update(
                mapOf(
                    "title" to task.title.trim().take(120),
                    "description" to task.description.trim().take(400),
                    "assignedMemberName" to task.assignedMemberName.trim().take(40),
                    "assignedMemberEmoji" to task.assignedMemberEmoji,
                    "dueDate" to task.dueDate.trim().take(40),
                    "dueEpochMillis" to task.dueEpochMillis.coerceAtLeast(0L),
                    "dueTime" to task.dueTime.trim().take(10).ifBlank { "09:00" },
                    "reminderMinutesList" to task.reminderMinutesList.distinct().take(10),
                    "priority" to task.priority,
                    "completed" to task.completed,
                    "category" to task.category.trim().take(40),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun setTaskCompleted(familyId: String, taskId: String, completed: Boolean): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("tasks").document(taskId)
        return try {
            requireUserId()
            docRef.update(mapOf("completed" to completed, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(familyId: String, taskId: String): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("tasks").document(taskId)
        return try {
            requireUserId()
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- MEAL PLANS ---

    fun observeMealPlans(familyId: String): Flow<List<MealPlan>> =
        observeCollection(familyId, "mealPlans", MealPlan::class.java)

    suspend fun upsertMealPlan(
        familyId: String,
        memberUids: List<String>,
        dayOfWeekIndex: Int,
        mealType: String,
        dishName: String,
        recipeId: String?
    ): Result<Unit> {
        val docId = "slot_${dayOfWeekIndex}_$mealType"
        val docRef = db.collection("families").document(familyId).collection("mealPlans").document(docId)
        return try {
            val uid = requireUserId()
            val trimmed = dishName.trim().take(120)
            if (trimmed.isBlank()) {
                val existing = docRef.get().await()
                if (existing.exists()) {
                    docRef.delete().await()
                }
            } else {
                val existing = docRef.get().await()
                if (existing.exists()) {
                    val updates = mutableMapOf<String, Any>(
                        "dayOfWeekIndex" to dayOfWeekIndex.coerceIn(0, 13),
                        "mealType" to mealType,
                        "dishName" to trimmed,
                        "notes" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (!recipeId.isNullOrBlank()) {
                        updates["recipeId"] = recipeId
                    } else {
                        updates["recipeId"] = FieldValue.delete()
                    }
                    docRef.update(updates).await()
                } else {
                    val payload = mutableMapOf<String, Any>(
                        "familyId" to familyId,
                        "dayOfWeekIndex" to dayOfWeekIndex.coerceIn(0, 13),
                        "mealType" to mealType,
                        "dishName" to trimmed,
                        "notes" to "",
                        "createdByUid" to uid,
                        "memberUids" to memberUids,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (!recipeId.isNullOrBlank()) {
                        payload["recipeId"] = recipeId
                    }
                    docRef.set(payload).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- RECIPES ---

    fun observeRecipes(familyId: String): Flow<List<Recipe>> =
        observeCollection(familyId, "recipes", Recipe::class.java)

    suspend fun addRecipe(
        familyId: String,
        memberUids: List<String>,
        recipe: Recipe
    ): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("recipes").document()
        return try {
            val uid = requireUserId()
            docRef.set(
                mapOf(
                    "familyId" to familyId,
                    "title" to recipe.title.trim().take(120),
                    "category" to recipe.category.trim().take(60),
                    "prepTimeMinutes" to recipe.prepTimeMinutes.coerceIn(1, 1440),
                    "servings" to recipe.servings.coerceIn(1, 50),
                    "ingredients" to recipe.ingredients.trim().take(3000),
                    "instructions" to recipe.instructions.trim().take(5000),
                    "emoji" to recipe.emoji,
                    "favorite" to false,
                    "createdByUid" to uid,
                    "memberUids" to memberUids,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateRecipe(familyId: String, recipe: Recipe): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("recipes").document(recipe.id)
        return try {
            requireUserId()
            require(recipe.id.isNotBlank())
            docRef.update(
                mapOf(
                    "title" to recipe.title.trim().take(120),
                    "category" to recipe.category.trim().take(60),
                    "prepTimeMinutes" to recipe.prepTimeMinutes.coerceIn(1, 1440),
                    "servings" to recipe.servings.coerceIn(1, 50),
                    "ingredients" to recipe.ingredients.trim().take(3000),
                    "instructions" to recipe.instructions.trim().take(5000),
                    "emoji" to recipe.emoji,
                    "favorite" to recipe.favorite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun setRecipeFavorite(familyId: String, recipeId: String, favorite: Boolean): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("recipes").document(recipeId)
        return try {
            requireUserId()
            docRef.update(mapOf("favorite" to favorite, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteRecipe(familyId: String, recipeId: String): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("recipes").document(recipeId)
        return try {
            requireUserId()
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- CUSTOMIZABLE CATEGORIES ---

    fun observeCategories(familyId: String): Flow<List<FamilyCategory>> =
        observeCollection(familyId, "categories", FamilyCategory::class.java)

    suspend fun seedDefaultCategoriesIfNeeded(
        familyId: String,
        memberUids: List<String>,
        onlyIfCompletelyEmpty: Boolean
    ): Result<Unit> {
        val colRef = db.collection("families").document(familyId).collection("categories")
        return try {
            val uid = requireUserId()
            val snap = colRef.whereArrayContains("memberUids", uid).get().await()
            if (onlyIfCompletelyEmpty && !snap.isEmpty) {
                return Result.success(Unit)
            }
            val existingIds = snap.documents.map { it.id }.toSet()
            val existingKeys = snap.documents.map {
                "${it.getString("scope").orEmpty()}:${it.getString("name").orEmpty().lowercase()}"
            }.toSet()

            val toSeed = DefaultCategories.all.filter { tpl ->
                val docId = "seed_${tpl.idSuffix}"
                val key = "${tpl.scope.name}:${tpl.name.lowercase()}"
                docId !in existingIds && key !in existingKeys
            }
            if (toSeed.isEmpty()) return Result.success(Unit)

            toSeed.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { tpl ->
                    val ref = colRef.document("seed_${tpl.idSuffix}")
                    batch.set(
                        ref,
                        mapOf(
                            "familyId" to familyId,
                            "scope" to tpl.scope.name,
                            "name" to tpl.name,
                            "emoji" to tpl.emoji,
                            "colorHex" to tpl.colorHex,
                            "createdByUid" to uid,
                            "memberUids" to memberUids,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "families/$familyId/categories")
            Result.failure(e)
        }
    }

    suspend fun addCategory(
        familyId: String,
        memberUids: List<String>,
        scope: CategoryScope,
        name: String,
        emoji: String,
        colorHex: String
    ): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("categories").document()
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(40)
            require(cleanName.isNotBlank()) { "El nombre de la categoría no puede estar vacío." }
            docRef.set(
                mapOf(
                    "familyId" to familyId,
                    "scope" to scope.name,
                    "name" to cleanName,
                    "emoji" to emoji.trim().ifBlank { "🏷️" }.take(16),
                    "colorHex" to colorHex.trim().ifBlank { "#1B4D3E" }.take(16),
                    "createdByUid" to uid,
                    "memberUids" to memberUids,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCategory(
        familyId: String,
        category: FamilyCategory,
        oldName: String
    ): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("categories").document(category.id)
        return try {
            val uid = requireUserId()
            require(category.id.isNotBlank())
            val cleanName = category.name.trim().take(40)
            require(cleanName.isNotBlank()) { "El nombre de la categoría no puede estar vacío." }
            docRef.update(
                mapOf(
                    "name" to cleanName,
                    "emoji" to category.emoji.trim().ifBlank { "🏷️" }.take(16),
                    "colorHex" to category.colorHex.trim().ifBlank { "#1B4D3E" }.take(16),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Propagate rename to matching items of that scope
            if (oldName.isNotBlank() && !oldName.equals(cleanName, ignoreCase = false)) {
                val targetCol = when (category.scope) {
                    CategoryScope.EVENT.name -> "calendarEvents"
                    CategoryScope.TASK.name -> "tasks"
                    CategoryScope.SHOPPING.name -> "shoppingItems"
                    CategoryScope.RECIPE.name -> "recipes"
                    else -> null
                }
                if (targetCol != null) {
                    val snap = db.collection("families")
                        .document(familyId)
                        .collection(targetCol)
                        .whereArrayContains("memberUids", uid)
                        .get()
                        .await()
                    val matchingRefs = snap.documents.filter {
                        it.getString("category")?.equals(oldName, ignoreCase = true) == true
                    }.map { it.reference }
                    matchingRefs.chunked(400).forEach { chunk ->
                        val batch = db.batch()
                        chunk.forEach { ref ->
                            batch.update(
                                ref,
                                mapOf(
                                    "category" to cleanName,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                        batch.commit().await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(familyId: String, categoryId: String): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("categories").document(categoryId)
        return try {
            requireUserId()
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- PRODUCT AUTOCOMPLETE HISTORY ---

    fun observeProductHistory(familyId: String): Flow<List<ProductHistoryEntry>> =
        observeCollection(familyId, "productHistory", ProductHistoryEntry::class.java)

    suspend fun recordProductHistory(
        familyId: String,
        memberUids: List<String>,
        name: String,
        unit: String,
        category: String,
        supermarket: String
    ): Result<Unit> {
        val cleanName = name.trim().take(120)
        if (cleanName.isBlank()) return Result.success(Unit)
        val cleanLower = cleanName.lowercase()
        val slug = cleanLower
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(80)
            .ifBlank { cleanLower.hashCode().toUInt().toString() }
        val docId = "prod_$slug"
        val docRef = db.collection("families").document(familyId).collection("productHistory").document(docId)
        return try {
            val uid = requireUserId()
            val cleanUnit = unit.trim().ifBlank { "ud" }.take(20)
            val cleanCat = category.trim().ifBlank { "Despensa" }.take(60)
            val cleanSuper = supermarket.trim().ifBlank { "Mercadona" }.take(40)

            val snap = docRef.get().await()
            if (snap.exists()) {
                val prevCount = (snap.getLong("useCount") ?: 0L).toInt()
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "nameLower" to cleanLower,
                        "lastUnit" to cleanUnit,
                        "lastCategory" to cleanCat,
                        "lastSupermarket" to cleanSuper,
                        "useCount" to (prevCount + 1).coerceIn(1, 100000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "familyId" to familyId,
                        "name" to cleanName,
                        "nameLower" to cleanLower,
                        "lastUnit" to cleanUnit,
                        "lastCategory" to cleanCat,
                        "lastSupermarket" to cleanSuper,
                        "useCount" to 1,
                        "createdByUid" to uid,
                        "memberUids" to memberUids,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- PET CARE (OLIVA FEEDING) ---

    fun observePetFeeding(familyId: String): Flow<List<PetFeedingStatus>> =
        observeCollection(familyId, "petCare", PetFeedingStatus::class.java)

    suspend fun toggleOlivaFedToday(
        familyId: String,
        memberUids: List<String>,
        memberName: String,
        currentlyFedToday: Boolean,
        dateKey: String,
        timeLabel: String
    ): Result<Boolean> {
        val docRef = db.collection("families").document(familyId).collection("petCare").document("oliva_feeding")
        return try {
            val uid = requireUserId()
            val newFed = !currentlyFedToday
            val cleanMember = memberName.trim().ifBlank { "Familia" }.take(40)
            val cleanTime = timeLabel.trim().take(16)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "petName" to "Oliva",
                        "dateKey" to dateKey,
                        "fed" to newFed,
                        "fedByMemberName" to if (newFed) cleanMember else "",
                        "fedTimeLabel" to if (newFed) cleanTime else "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "familyId" to familyId,
                        "petName" to "Oliva",
                        "dateKey" to dateKey,
                        "fed" to newFed,
                        "fedByMemberName" to if (newFed) cleanMember else "",
                        "fedTimeLabel" to if (newFed) cleanTime else "",
                        "createdByUid" to uid,
                        "memberUids" to memberUids,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(newFed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- MACROS (TARGETS & INGREDIENTS) ---

    fun observeMacroTargets(familyId: String): Flow<List<MacroTargets>> =
        observeCollection(familyId, "macroTargets", MacroTargets::class.java)

    suspend fun saveMacroTargets(
        familyId: String,
        memberUids: List<String>,
        targets: MacroTargets
    ): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("macroTargets").document("default")
        return try {
            val uid = requireUserId()
            val dC = targets.dailyCarbsG.coerceIn(0.0, 2000.0)
            val dP = targets.dailyProteinG.coerceIn(0.0, 1000.0)
            val dF = targets.dailyFatG.coerceIn(0.0, 1000.0)
            val bC = targets.breakfastCarbsG.coerceIn(0.0, 1000.0)
            val bP = targets.breakfastProteinG.coerceIn(0.0, 1000.0)
            val bF = targets.breakfastFatG.coerceIn(0.0, 1000.0)
            val lC = targets.lunchCarbsG.coerceIn(0.0, 1000.0)
            val lP = targets.lunchProteinG.coerceIn(0.0, 1000.0)
            val lF = targets.lunchFatG.coerceIn(0.0, 1000.0)
            val nC = targets.dinnerCarbsG.coerceIn(0.0, 1000.0)
            val nP = targets.dinnerProteinG.coerceIn(0.0, 1000.0)
            val nF = targets.dinnerFatG.coerceIn(0.0, 1000.0)

            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "dailyCarbsG" to dC,
                        "dailyProteinG" to dP,
                        "dailyFatG" to dF,
                        "breakfastCarbsG" to bC,
                        "breakfastProteinG" to bP,
                        "breakfastFatG" to bF,
                        "lunchCarbsG" to lC,
                        "lunchProteinG" to lP,
                        "lunchFatG" to lF,
                        "dinnerCarbsG" to nC,
                        "dinnerProteinG" to nP,
                        "dinnerFatG" to nF,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "familyId" to familyId,
                        "dailyCarbsG" to dC,
                        "dailyProteinG" to dP,
                        "dailyFatG" to dF,
                        "breakfastCarbsG" to bC,
                        "breakfastProteinG" to bP,
                        "breakfastFatG" to bF,
                        "lunchCarbsG" to lC,
                        "lunchProteinG" to lP,
                        "lunchFatG" to lF,
                        "dinnerCarbsG" to nC,
                        "dinnerProteinG" to nP,
                        "dinnerFatG" to nF,
                        "createdByUid" to uid,
                        "memberUids" to memberUids,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeMacroIngredients(familyId: String): Flow<List<MacroIngredient>> =
        observeCollection(familyId, "macroIngredients", MacroIngredient::class.java)

    suspend fun seedDefaultMacroIngredientsIfNeeded(
        familyId: String,
        memberUids: List<String>,
        onlyIfCompletelyEmpty: Boolean
    ): Result<Unit> {
        val colRef = db.collection("families").document(familyId).collection("macroIngredients")
        return try {
            val uid = requireUserId()
            val snap = colRef.whereArrayContains("memberUids", uid).get().await()
            if (onlyIfCompletelyEmpty && !snap.isEmpty) {
                return Result.success(Unit)
            }
            val existingIds = snap.documents.map { it.id }.toSet()
            val existingNames = snap.documents.map {
                it.getString("name").orEmpty().trim().lowercase()
            }.toSet()

            val toSeed = DefaultMacroIngredients.all.filter { tpl ->
                val docId = "seed_${tpl.idSuffix}"
                docId !in existingIds && tpl.name.lowercase() !in existingNames
            }
            if (toSeed.isEmpty()) return Result.success(Unit)

            toSeed.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { tpl ->
                    val ref = colRef.document("seed_${tpl.idSuffix}")
                    batch.set(
                        ref,
                        mapOf(
                            "familyId" to familyId,
                            "name" to tpl.name.take(80),
                            "emoji" to tpl.emoji.take(16),
                            "carbsPer100g" to tpl.carbsPer100g.coerceIn(0.0, 100.0),
                            "proteinPer100g" to tpl.proteinPer100g.coerceIn(0.0, 100.0),
                            "fatPer100g" to tpl.fatPer100g.coerceIn(0.0, 100.0),
                            "createdByUid" to uid,
                            "memberUids" to memberUids,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "families/$familyId/macroIngredients")
            Result.failure(e)
        }
    }

    suspend fun addMacroIngredient(
        familyId: String,
        memberUids: List<String>,
        name: String,
        emoji: String,
        carbsPer100g: Double,
        proteinPer100g: Double,
        fatPer100g: Double
    ): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("macroIngredients").document()
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(80)
            require(cleanName.isNotBlank()) { "El nombre del ingrediente no puede estar vacío." }
            docRef.set(
                mapOf(
                    "familyId" to familyId,
                    "name" to cleanName,
                    "emoji" to emoji.trim().ifBlank { "🥗" }.take(16),
                    "carbsPer100g" to carbsPer100g.coerceIn(0.0, 100.0),
                    "proteinPer100g" to proteinPer100g.coerceIn(0.0, 100.0),
                    "fatPer100g" to fatPer100g.coerceIn(0.0, 100.0),
                    "createdByUid" to uid,
                    "memberUids" to memberUids,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateMacroIngredient(familyId: String, ingredient: MacroIngredient): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("macroIngredients").document(ingredient.id)
        return try {
            requireUserId()
            require(ingredient.id.isNotBlank())
            val cleanName = ingredient.name.trim().take(80)
            require(cleanName.isNotBlank()) { "El nombre del ingrediente no puede estar vacío." }
            docRef.update(
                mapOf(
                    "name" to cleanName,
                    "emoji" to ingredient.emoji.trim().ifBlank { "🥗" }.take(16),
                    "carbsPer100g" to ingredient.carbsPer100g.coerceIn(0.0, 100.0),
                    "proteinPer100g" to ingredient.proteinPer100g.coerceIn(0.0, 100.0),
                    "fatPer100g" to ingredient.fatPer100g.coerceIn(0.0, 100.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMacroIngredient(familyId: String, ingredientId: String): Result<Unit> {
        val docRef = db.collection("families").document(familyId).collection("macroIngredients").document(ingredientId)
        return try {
            requireUserId()
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
