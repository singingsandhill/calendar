package me.singingsandhill.calendar.domain.owner;

import java.util.Optional;

public interface OwnerRepository {

    Optional<Owner> findById(String ownerId);

    Owner save(Owner owner);

    boolean existsById(String ownerId);
}
