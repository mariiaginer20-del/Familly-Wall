package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AddHome
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.FamilyWallViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FamilySetupScreen(
    viewModel: FamilyWallViewModel,
    defaultDisplayName: String,
    userEmail: String?,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBusy by viewModel.familySetupBusy.collectAsStateWithLifecycle()
    val errorMsg by viewModel.familySetupError.collectAsStateWithLifecycle()

    var modeTab by remember { mutableIntStateOf(0) } // 0 = Crear hogar, 1 = Unirme con código
    var familyName by remember { mutableStateOf("Nuestro Hogar") }
    var inviteCode by remember { mutableStateOf("") }
    var memberName by remember {
        mutableStateOf(
            defaultDisplayName.trim().split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "Yo"
        )
    }
    var selectedEmoji by remember { mutableStateOf("👩") }
    var selectedColorHex by remember { mutableStateOf("#1B4D3E") }
    var seedStarter by remember { mutableStateOf(true) }

    val avatarOptions = listOf("👩", "👨", "🧑", "👧", "👦", "🧓", "🏡", "⭐")
    val colorOptions = listOf("#1B4D3E", "#C25934", "#2847D6", "#0D9488", "#8C6239", "#6D3FB8")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userEmail?.uppercase() ?: "CUENTA CONECTADA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Configura tu unidad familiar",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                TextButton(onClick = onSignOut) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = "Cerrar sesión",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Salir")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Crea un nuevo muro familiar para recibir un código de invitación, o introduce el código que te ha pasado tu pareja.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Switcher: Crear vs Unirse
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isCreate = modeTab == 0
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCreate) MaterialTheme.colorScheme.surface else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                modeTab = 0
                                viewModel.clearFamilySetupError()
                            }
                            .testTag("setup_tab_create")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.AddHome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isCreate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Crear familia",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isCreate) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isCreate) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isCreate) MaterialTheme.colorScheme.surface else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                modeTab = 1
                                viewModel.clearFamilySetupError()
                            }
                            .testTag("setup_tab_join")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.GroupAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (!isCreate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tengo un código",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (!isCreate) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (!isCreate) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (modeTab == 0) {
                        OutlinedTextField(
                            value = familyName,
                            onValueChange = { familyName = it },
                            label = { Text("Nombre de tu unidad familiar") },
                            placeholder = { Text("ej: Familia García, Casa Centro") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_family_name_input")
                        )
                    } else {
                        OutlinedTextField(
                            value = inviteCode,
                            onValueChange = { inviteCode = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(10) },
                            label = { Text("Código de invitación de tu pareja") },
                            placeholder = { Text("ej: A7K9P2") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("join_invite_code_input")
                        )
                    }

                    OutlinedTextField(
                        value = memberName,
                        onValueChange = { memberName = it },
                        label = { Text("Tu nombre en la familia") },
                        placeholder = { Text("ej: María, Álex, Papá...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_member_name_input")
                    )

                    Column {
                        Text(
                            text = "Tu icono",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            avatarOptions.forEach { emoji ->
                                val picked = selectedEmoji == emoji
                                Surface(
                                    shape = CircleShape,
                                    color = if (picked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .clickable { selectedEmoji = emoji }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = emoji, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "Tu color en la agenda",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            colorOptions.forEach { hex ->
                                val c = runCatching {
                                    Color(android.graphics.Color.parseColor(hex))
                                }.getOrDefault(MaterialTheme.colorScheme.primary)
                                val picked = selectedColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .then(
                                            if (picked) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                            else Modifier
                                        )
                                        .clickable { selectedColorHex = hex }
                                )
                            }
                        }
                    }

                    if (modeTab == 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Incluir ejemplo inicial",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Añade un producto, tarea y receta de muestra",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = seedStarter,
                                onCheckedChange = { seedStarter = it }
                            )
                        }
                    }

                    val activeErrorMsg = errorMsg
                    if (activeErrorMsg != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activeErrorMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (modeTab == 0) {
                                viewModel.createFamilyUnit(
                                    familyName = familyName,
                                    memberName = memberName,
                                    avatarEmoji = selectedEmoji,
                                    colorHex = selectedColorHex,
                                    seedStarterData = seedStarter
                                )
                            } else {
                                viewModel.joinFamilyByCode(
                                    code = inviteCode,
                                    memberName = memberName,
                                    avatarEmoji = selectedEmoji,
                                    colorHex = selectedColorHex
                                )
                            }
                        },
                        enabled = !isBusy && memberName.isNotBlank() && (if (modeTab == 0) familyName.isNotBlank() else inviteCode.length >= 4),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag(if (modeTab == 0) "create_family_button" else "join_family_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (modeTab == 0) "Crear unidad familiar" else "Unirme a esta familia",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
