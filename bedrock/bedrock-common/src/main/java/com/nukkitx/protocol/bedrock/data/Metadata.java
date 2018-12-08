package com.nukkitx.protocol.bedrock.data;

import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Metadata {
    FLAGS(Type.LONG),
    HEALTH(Type.INT),
    VARIANT(Type.INT),
    COLOR(Type.BYTE),
    NAMETAG(Type.STRING),
    OWNER_EID(Type.LONG),
    TARGET_EID(Type.LONG),
    AIR(Type.SHORT),
    POTION_COLOR(Type.INT),
    POTION_AMBIENT(Type.BYTE),
    HURT_TIME(Type.INT),
    JUMP_DURATION(Type.BYTE),
    HURT_DIRECTION(Type.INT),
    PADDLE_TIME_LEFT(Type.FLOAT),
    PADDLE_TIME_RIGHT(Type.FLOAT),
    EXPERIENCE_VALUE(Type.INT),
    DISPLAY_ITEM(Type.INT),
    DISPLAY_OFFSET(null), // Can be long or int
    HAS_DISPLAY(Type.BYTE),
    CHARGED(Type.BYTE),
    ENDERMAN_HELD_ITEM_ID(Type.INT),
    ENTITY_AGE(Type.BYTE),
    //WITCH_UNKNOWN(Type.BYTE), // TODO: Initialized to 0 but never changed.
    CAN_START_SLEEP(Type.BYTE),
    PLAYER_INDEX(Type.INT),
    BED_RESPAWN_POS(Type.VECTOR3I),
    FIREBALL_POWER_X(Type.FLOAT),
    FIREBALL_POWER_Y(Type.FLOAT),
    FIREBALL_POWER_Z(Type.FLOAT),
    POTION_AUX_VALUE(Type.SHORT),
    LEAD_HOLDER_EID(Type.LONG),
    SCALE(Type.FLOAT),
    INTERACTIVE_TAG(Type.STRING),
    NPC_SKIN_ID(Type.STRING),
    URL_TAG(Type.STRING),
    MAX_AIR(Type.SHORT),
    MARK_VARIANT(Type.INT),
    CONTAINER_TYPE(Type.BYTE),
    CONTAINER_BASE_SIZE(Type.INT),
    CONTAINER_EXTRA_SLOTS_PER_STRENGTH(Type.INT),
    BLOCK_TARGET(Type.VECTOR3I),
    WITHER_INVULNERABLE_TICKS(Type.INT),
    WITHER_TARGET_1(Type.LONG),
    WITHER_TARGET_2(Type.LONG),
    WITHER_TARGET_3(Type.LONG),
    WITHER_AERIAL_ATTACK(Type.SHORT),
    BOUNDING_BOX_WIDTH(Type.FLOAT),
    BOUNDING_BOX_HEIGHT(Type.FLOAT),
    FUSE_LENGTH(Type.INT),
    RIDER_SEAT_POSITION(Type.VECTOR3F),
    RIDER_ROTATION_LOCKED(Type.BYTE),
    RIDER_MAX_ROTATION(Type.FLOAT),
    RIDER_MIN_ROTATION(Type.FLOAT),
    AREA_EFFECT_CLOUD_RADIUS(Type.FLOAT),
    AREA_EFFECT_CLOUD_WAITING(Type.INT),
    AREA_EFFECT_CLOUD_PARTICLE_ID(Type.INT),
    SHULKER_PEAK_HEIGHT(Type.INT),
    SHULKER_ATTACH_FACE(Type.BYTE),
    SHULKER_ATTACH_POS(Type.VECTOR3I),
    TRADING_PLAYER_EID(Type.LONG),
    COMMAND_BLOCK_ENABLED(Type.BYTE),
    COMMAND_BLOCK_COMMAND(Type.STRING),
    COMMAND_BLOCK_LAST_OUTPUT(Type.STRING),
    COMMAND_BLOCK_TRACK_OUTPUT(Type.BYTE),
    CONTROLLING_RIDER_SEAT_NUMBER(Type.BYTE),
    STRENGTH(Type.INT),
    MAX_STRENGTH(Type.INT),
    EVOKER_SPELL_COLOR(Type.INT),
    LIMITED_LIFE(Type.INT),
    ARMOR_STAND_POSE_INDEX(Type.INT),
    ENDER_CRYSTAL_TIME_OFFSET(Type.INT),
    ALWAYS_SHOW_NAMETAG(Type.BYTE),
    COLOR_2(Type.BYTE),
    SCORE_TAG(Type.STRING),
    BALLOON_ATTACHED_ENTITY(Type.LONG),
    PUFFERFISH_SIZE(Type.BYTE),
    AGENT_ID(Type.LONG);

    private final Type type;

    public enum Flag {
        ON_FIRE,
        SNEAKING,
        RIDING,
        SPRINTING,
        ACTION,
        INVISIBLE,
        TEMPTED,
        IN_LOVE,
        SADDLED,
        POWERED,
        IGNITED,
        BABY,
        CONVERTING,
        CRITICAL,
        CAN_SHOW_NAMETAG,
        ALWAYS_SHOW_NAMETAG,
        IMMOBILE,
        SILENT,
        WALLCLIMBING,
        CAN_CLIMB,
        SWIMMER,
        CAN_FLY,
        WALKER,
        RESTING,
        SITTING,
        ANGRY,
        INTERESTED,
        CHARGED,
        TAMED,
        ORPHANED,
        LEASHED,
        SHEARED,
        GLIDING,
        ELDER,
        MOVING,
        BREATHING,
        CHESTED,
        STACKABLE,
        SHOW_BASE,
        REARING,
        VIBRATING,
        IDLING,
        EVOKER_SPELL,
        CHARGE_ATTACK,
        WASD_CONTROLLED,
        CAN_POWER_JUMP,
        LINGER,
        HAS_COLLISION,
        AFFECTED_BY_GRAVITY,
        FIRE_IMMUNE,
        DANCING,
        ENCHANTED,
        SHOW_TRIDENT_ROPE,
        CONTAINER_PRIVATE,
        TRANSFORM,
        SPIN_ATTACK,
        SWIMMING,
        BRIBED,
        PREGNANT,
        LAYING_EGG
    }

    @RequiredArgsConstructor
    public enum Type {
        BYTE(Byte.class),
        SHORT(Short.class),
        INT(Integer.class),
        FLOAT(Float.class),
        STRING(String.class),
        ITEM(Item.class),
        VECTOR3I(Vector3i.class),
        LONG(Long.class),
        VECTOR3F(Vector3f.class);

        private final Class clazz;

        public Class typeClass() {
            return clazz;
        }
    }
}
