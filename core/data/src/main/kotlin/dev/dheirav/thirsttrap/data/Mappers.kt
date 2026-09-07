package dev.dheirav.thirsttrap.data

import dev.dheirav.thirsttrap.data.entity.CareEventEntity
import dev.dheirav.thirsttrap.data.entity.PlantEntity
import dev.dheirav.thirsttrap.domain.Anchors
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.CheckResult
import dev.dheirav.thirsttrap.domain.Medium
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantSource
import dev.dheirav.thirsttrap.domain.PlantStatus
import dev.dheirav.thirsttrap.domain.WateringMethod

/**
 * Boilerplate, and the price of decision D2.
 *
 * The domain module cannot see Room, so its entities are separate classes and
 * something has to bridge them. That is the cost of keeping the Android SDK off
 * the domain classpath - paid here, once, in one file.
 */

/**
 * Enums are stored as TEXT, never ordinals: ordinals break the moment the enum
 * is reordered and make an export unreadable. Every decode falls back to an
 * UNKNOWN member rather than throwing, so a future version's event type cannot
 * crash an older parser reading a backup.
 */
private inline fun <reified T : Enum<T>> decode(raw: String?, fallback: T): T =
    raw?.let { value -> enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) } }
        ?: fallback

fun PlantEntity.toDomain(): Plant = Plant(
    id = id,
    name = name,
    species = species,
    medium = decode(medium, Medium.UNKNOWN),
    location = location,
    status = decode(status, PlantStatus.UNKNOWN),
    containerDesc = containerDesc,
    potDiameterCm = potDiameterCm,
    hasDrainage = hasDrainage,
    source = decode(source, PlantSource.UNKNOWN),
    acquiredEpochDay = acquiredDate,
    targetDryness = targetDryness,
    defaultWaterMl = defaultWaterMl,
    lightNeeds = lightNeeds,
    fertilizerCadenceDays = fertilizerCadenceDays,
    depletionTrigger = depletionTrigger,
    anchors = if (wetAnchorG != null && dryAnchorG != null) {
        Anchors(wetAnchorG, dryAnchorG, dryAnchorProvisional)
    } else {
        null
    },
    slopeEwmaGramsPerDay = slopeEwmaGPerDay,
    needsRecalibration = needsRecalibration,
    archived = archived,
    coverPhotoId = coverPhotoId,
    propagationStage = propagationStage?.let {
        runCatching { dev.dheirav.thirsttrap.domain.PropagationStage.valueOf(it.uppercase()) }.getOrNull()
    },
    propagationStageSinceMillis = propagationStageSince,
)

fun Plant.toEntity(createdAt: Long, updatedAt: Long): PlantEntity = PlantEntity(
    id = id,
    name = name,
    species = species,
    acquiredDate = acquiredEpochDay,
    source = source.name,
    medium = medium.name,
    location = location,
    status = status.name,
    containerDesc = containerDesc,
    potDiameterCm = potDiameterCm,
    hasDrainage = hasDrainage,
    targetDryness = targetDryness,
    defaultWaterMl = defaultWaterMl,
    lightNeeds = lightNeeds,
    fertilizerCadenceDays = fertilizerCadenceDays,
    depletionTrigger = depletionTrigger,
    wetAnchorG = anchors?.wetGrams,
    dryAnchorG = anchors?.dryGrams,
    dryAnchorProvisional = anchors?.dryIsProvisional ?: true,
    slopeEwmaGPerDay = slopeEwmaGramsPerDay,
    needsRecalibration = needsRecalibration,
    archived = archived,
    coverPhotoId = coverPhotoId,
    propagationStage = propagationStage?.name,
    propagationStageSince = propagationStageSinceMillis,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun CareEventEntity.toDomain(): CareEvent = CareEvent(
    id = id,
    plantId = plantId,
    timestampMillis = timestamp,
    tzOffsetMinutes = tzOffsetMinutes,
    type = decode(type, CareEventType.UNKNOWN),
    note = note,
    amountMl = amountMl,
    method = method?.let { decode(it, WateringMethod.UNKNOWN) },
    checkResult = checkResult?.let { decode(it, CheckResult.UNKNOWN) },
    fertilizerName = fertilizerName,
    dilution = dilution,
    fromMedium = fromMedium?.let { decode(it, Medium.UNKNOWN) },
    toMedium = toMedium?.let { decode(it, Medium.UNKNOWN) },
    cause = cause,
)

fun CareEvent.toEntity(createdAt: Long, updatedAt: Long): CareEventEntity = CareEventEntity(
    id = id,
    plantId = plantId,
    timestamp = timestampMillis,
    tzOffsetMinutes = tzOffsetMinutes,
    type = type.name,
    note = note,
    amountMl = amountMl,
    method = method?.name,
    checkResult = checkResult?.name,
    fertilizerName = fertilizerName,
    dilution = dilution,
    fromMedium = fromMedium?.name,
    toMedium = toMedium?.name,
    cause = cause,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ---- export / import ----

fun dev.dheirav.thirsttrap.data.entity.ReminderEntity.toDomainReminder(): dev.dheirav.thirsttrap.domain.Reminder =
    dev.dheirav.thirsttrap.domain.Reminder(
        id = id,
        plantId = plantId,
        kind = if (kind.equals("TASK", true)) {
            dev.dheirav.thirsttrap.domain.ReminderKind.TASK
        } else {
            dev.dheirav.thirsttrap.domain.ReminderKind.CHECK
        },
        title = title,
        intervalDays = intervalDays,
        nextDueAtMillis = nextDueAt,
        enabled = enabled,
        snoozedUntilMillis = snoozedUntil,
        lastFiredAtMillis = lastFiredAt,
    )

fun dev.dheirav.thirsttrap.domain.Reminder.toReminderEntity(createdAt: Long): dev.dheirav.thirsttrap.data.entity.ReminderEntity =
    dev.dheirav.thirsttrap.data.entity.ReminderEntity(
        id = id,
        plantId = plantId,
        kind = kind.name,
        title = title,
        intervalDays = intervalDays,
        nextDueAt = nextDueAtMillis,
        enabled = enabled,
        snoozedUntil = snoozedUntilMillis,
        lastFiredAt = lastFiredAtMillis,
        createdAt = createdAt,
    )

fun dev.dheirav.thirsttrap.data.entity.PhotoEntity.toDomain(): dev.dheirav.thirsttrap.domain.Photo =
    dev.dheirav.thirsttrap.domain.Photo(
        id = id,
        plantId = plantId,
        careEventId = careEventId,
        relativePath = relativePath,
        takenAtMillis = takenAt,
        tzOffsetMinutes = tzOffsetMinutes,
        widthPx = widthPx,
        heightPx = heightPx,
        bytes = bytes,
        caption = caption,
    )

fun dev.dheirav.thirsttrap.domain.Photo.toPhotoEntity(createdAt: Long): dev.dheirav.thirsttrap.data.entity.PhotoEntity =
    dev.dheirav.thirsttrap.data.entity.PhotoEntity(
        id = id,
        plantId = plantId,
        careEventId = careEventId,
        relativePath = relativePath,
        takenAt = takenAtMillis,
        tzOffsetMinutes = tzOffsetMinutes,
        widthPx = widthPx,
        heightPx = heightPx,
        bytes = bytes,
        caption = caption,
        createdAt = createdAt,
    )

fun dev.dheirav.thirsttrap.data.entity.WeightReadingEntity.toDomainReading(): dev.dheirav.thirsttrap.domain.WeightReading =
    dev.dheirav.thirsttrap.domain.WeightReading(
        id = id,
        plantId = plantId,
        timestampMillis = timestamp,
        grams = grams,
        context = runCatching {
            dev.dheirav.thirsttrap.domain.ReadingContext.valueOf(context.uppercase())
        }.getOrDefault(dev.dheirav.thirsttrap.domain.ReadingContext.ROUTINE),
        excluded = excluded,
    )
