package com.github.cerealklla.coryspondance.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;

/** A player's own inbox -- the whole attachment payload, see {@code registration.ModAttachments#PLAYER_MAIL}. */
public record PlayerMail(List<Letter> letters) {

    public static final PlayerMail EMPTY = new PlayerMail(List.of());

    public static final Codec<PlayerMail> CODEC = Letter.CODEC.listOf().xmap(PlayerMail::new, PlayerMail::letters);

    public Optional<Letter> find(UUID letterId) {
        return letters.stream().filter(l -> l.id().equals(letterId)).findFirst();
    }

    public PlayerMail withAdded(Letter letter) {
        List<Letter> updated = new ArrayList<>(letters);
        updated.add(letter);
        return new PlayerMail(List.copyOf(updated));
    }

    public PlayerMail withReplaced(Letter letter) {
        List<Letter> updated = new ArrayList<>(letters);
        for (int i = 0; i < updated.size(); i++) {
            if (updated.get(i).id().equals(letter.id())) {
                updated.set(i, letter);
                return new PlayerMail(List.copyOf(updated));
            }
        }
        return this; // No such letter -- no-op rather than silently appending it.
    }

    public PlayerMail withRemoved(UUID letterId) {
        List<Letter> updated = new ArrayList<>(letters);
        updated.removeIf(l -> l.id().equals(letterId));
        return new PlayerMail(List.copyOf(updated));
    }
}
