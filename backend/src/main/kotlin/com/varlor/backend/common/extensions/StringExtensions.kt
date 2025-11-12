package com.varlor.backend.common.extensions

import java.util.Locale

/**
 * Normalise un email en minuscules selon la locale par défaut.
 * 
 * Cette extension garantit une normalisation cohérente des adresses email
 * dans tout le projet, en convertissant la chaîne en minuscules selon
 * la locale par défaut du système.
 * 
 * @return l'email normalisé en minuscules
 * 
 * @example
 * ```
 * val email = "User@Example.COM"
 * val normalized = email.normalizeEmail() // "user@example.com"
 * ```
 */
fun String.normalizeEmail(): String = this.lowercase(Locale.getDefault())

