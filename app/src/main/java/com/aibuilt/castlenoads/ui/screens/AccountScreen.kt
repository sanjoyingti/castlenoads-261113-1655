package com.aibuilt.castlenoads.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibuilt.castlenoads.model.QUALITY_OPTIONS
import com.aibuilt.castlenoads.premium.Entitlements
import com.aibuilt.castlenoads.ui.CastleViewModel

@Composable
fun AccountScreen(
    vm: CastleViewModel,
    onOpenPremium: () -> Unit,
    onCast: () -> Unit
) {
    val profiles by vm.profiles.collectAsState()
    val activeId by vm.activeProfileId.collectAsState()
    val settings by vm.settings.collectAsState()
    val castName by vm.castDeviceName.collectAsState()
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var kidProfile by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("You", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(14.dp))

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenPremium)
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        Entitlements.planName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Active — everything unlocked, no ads",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    "Details",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Profiles", style = MaterialTheme.typography.titleMedium)
        Text(
            "${profiles.size} of ${Entitlements.maxProfiles()} used",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(profiles, key = { it.id }) { profile ->
                val isActive = profile.id == activeId
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(84.dp)
                        .clickable { vm.selectProfile(profile.id) }
                ) {
                    Box(
                        Modifier
                            .size(60.dp)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            CastleViewModel.AVATARS.getOrElse(profile.avatarIndex) { "🛡️" },
                            fontSize = 26.sp
                        )
                    }
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    if (isActive) {
                        Text(
                            "Active",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (profiles.size < Entitlements.maxProfiles()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(84.dp)
                            .clickable {
                                newName = ""
                                kidProfile = false
                                addError = null
                                adding = true
                            }
                    ) {
                        Box(
                            Modifier
                                .size(60.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add profile")
                        }
                        Text("Add", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (profiles.size > 1) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Delete the active profile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { vm.deleteProfile(activeId) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete profile")
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Playback", style = MaterialTheme.typography.titleMedium)
        SettingSwitch(
            icon = { Icon(Icons.Filled.Hd, contentDescription = null) },
            title = "Dark theme",
            subtitle = "Cinema look, easier on the eyes at night",
            checked = settings.darkTheme
        ) { vm.updateSettings { s -> s.copy(darkTheme = it) } }
        SettingSwitch(
            icon = { Icon(Icons.Filled.Subtitles, contentDescription = null) },
            title = "Subtitles by default",
            subtitle = "Turn on whenever a subtitle track exists",
            checked = settings.subtitlesOn
        ) { vm.updateSettings { s -> s.copy(subtitlesOn = it) } }
        SettingSwitch(
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            title = "Autoplay next episode",
            subtitle = "Roll straight into the next one",
            checked = settings.autoplayNext
        ) { vm.updateSettings { s -> s.copy(autoplayNext = it) } }
        SettingSwitch(
            icon = { Icon(Icons.Filled.Download, contentDescription = null) },
            title = "Download over Wi-Fi only",
            subtitle = "Protect your mobile data",
            checked = settings.wifiOnlyDownloads
        ) { vm.updateSettings { s -> s.copy(wifiOnlyDownloads = it) } }
        SettingSwitch(
            icon = { Icon(Icons.Filled.Wifi, contentDescription = null) },
            title = "Ultra HD 4K unlocked",
            subtitle = "4K is offered in every quality menu",
            checked = true
        ) { }

        Spacer(Modifier.height(14.dp))
        Text("Default quality", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(QUALITY_OPTIONS) { option ->
                val selected = settings.preferredQuality == option
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable {
                        vm.updateSettings { s -> s.copy(preferredQuality = option) }
                    }
                ) {
                    Text(
                        option,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCast)
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Cast, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Cast to TV", style = MaterialTheme.typography.titleMedium)
                    Text(
                        castName ?: "No device connected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Continue watching", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Clear the resume row on the home screen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { vm.clearProgress() }) { Text("Clear history") }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Castle · patched build · no ads, premium active",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }

    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("New profile") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                            addError = null
                        },
                        placeholder = { Text("Name") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = kidProfile, onCheckedChange = { kidProfile = it })
                        Spacer(Modifier.width(10.dp))
                        Text("Kids profile")
                    }
                    if (addError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            addError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isBlank()) {
                        addError = "Give the profile a name."
                    } else {
                        vm.addProfile(newName, kidProfile) { added ->
                            if (added) adding = false else addError = "Profile limit reached."
                        }
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { adding = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingSwitch(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun UnusedCheck() {
    Icon(Icons.Filled.Check, contentDescription = null)
}
