package ru.ctf.dao;

import ru.ctf.model.Command;

public interface CommandDao extends Dao<Command> {
    Command findById(Long id);
    Command findByInstructionId(String instructionId);
}
