package com.github.cerealklla.coryspondance.registration;

import java.util.function.Supplier;

import com.github.cerealklla.coryspondance.CoryspondanceMod;
import com.github.cerealklla.coryspondance.mail.PlayerMail;

import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {

    private ModAttachments() {
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CoryspondanceMod.MODID);

    // copyOnDeath() required -- mail must survive death (a brand-new Player entity is created on
    // respawn, and NeoForge attachments don't carry over by default). sync() is required for the
    // client's own MailboxScreen to see the inbox at all with zero extra network round-trip, same
    // shape as Myssions' own QUEST_DATA attachment.
    public static final Supplier<AttachmentType<PlayerMail>> PLAYER_MAIL = ATTACHMENT_TYPES.register(
            "player_mail",
            () -> AttachmentType.builder(holder -> PlayerMail.EMPTY)
                    .serialize(PlayerMail.CODEC.fieldOf("data"))
                    .copyOnDeath()
                    .sync(ByteBufCodecs.fromCodecWithRegistries(PlayerMail.CODEC))
                    .build());
}
