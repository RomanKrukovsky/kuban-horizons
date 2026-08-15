package dev.romankrukovsky.kubanhorizons.vessel;

import net.minecraft.resources.ResourceLocation;
import java.util.function.Supplier;

/**
 * Типы живых сосудов (Vessels) — артефактов с собственным сознанием.
 * Каждый тип даёт доступ к своей школе магии.
 */
public enum VesselType {
    /** Лампа — исполнение желаний (wish execution) */
    LAMP(
        "lamp",
        () -> ResourceLocation.fromNamespaceAndPath("kubanhorizons", "textures/item/vessel_lamp.png"),
        School.WISH_EXECUTION,
        "Лампа исполняет желания через зарегистрированные capability."
    ),

    /** Зеркало — иллюзии и альтернативные миры */
    MIRROR(
        "mirror",
        () -> ResourceLocation.fromNamespaceAndPath("kubanhorizons", "textures/item/vessel_mirror.png"),
        School.ILLUSION_ALTERNATE,
        "Зеркало открывает иллюзии и карманные измерения."
    ),

    /** Кольцо — персональная магия (усиление владельца) */
    RING(
        "ring",
        () -> ResourceLocation.fromNamespaceAndPath("kubanhorizons", "textures/item/vessel_ring.png"),
        School.PERSONAL_MAGIC,
        "Кольцо усиливает личные способности владельца."
    ),

    /** Кувшин — создание существ */
    JUG(
        "jug",
        () -> ResourceLocation.fromNamespaceAndPath("kubanhorizons", "textures/item/vessel_jug.png"),
        School.CREATURE_CREATION,
        "Кувшин порождает и связывает существ."
    ),

    /** Музыкальная шкатулка — эмоции и атмосфера */
    MUSIC_BOX(
        "music_box",
        () -> ResourceLocation.fromNamespaceAndPath("kubanhorizons", "textures/item/vessel_music_box.png"),
        School.EMOTION_ATMOSPHERE,
        "Шкатулка управляет эмоциональным состоянием и атмосферой."
    );

    public enum School {
        WISH_EXECUTION,      // Лампа
        ILLUSION_ALTERNATE,  // Зеркало
        PERSONAL_MAGIC,      // Кольцо
        CREATURE_CREATION,   // Кувшин
        EMOTION_ATMOSPHERE   // Шкатулка
    }

    private final String id;
    private final Supplier<ResourceLocation> texture;
    private final School school;
    private final String description;

    VesselType(String id, Supplier<ResourceLocation> texture, School school, String description) {
        this.id = id;
        this.texture = texture;
        this.school = school;
        this.description = description;
    }

    public String getId() { return id; }
    public ResourceLocation getTexture() { return texture.get(); }
    public School getSchool() { return school; }
    public String getDescription() { return description; }

    public static VesselType fromId(String id) {
        for (VesselType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return LAMP; // default
    }
}
