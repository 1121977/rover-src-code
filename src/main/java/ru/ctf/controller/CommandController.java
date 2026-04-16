package ru.ctf.controller;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ctf.dao.CommandDao;
import ru.ctf.model.Command;

@RestController
public class CommandController {

    @Autowired
    CommandDao commandDao;

    @Autowired
    Logger logger;

    @PutMapping(value = "/api/put")
    public ResponseEntity<Void> saveCommand(@RequestBody Command command) {
        if (command == null || command.getInstruction() == null || command.getInstructionId() == null
                || command.getInstruction().isBlank() || command.getInstructionId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        commandDao.save(command);
        logger.info("Method: PUT; Instruction: {}; InstructionID: {}",
                getPreview(command.getInstruction()), getPreview(command.getInstructionId()));
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/api/get/{instructionId}", produces="application/json")
    public ResponseEntity<Command> getCommand(@PathVariable("instructionId") String instructionId) {
        if (instructionId == null || instructionId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        logger.info("Method: GET; InstructionID: {}", getPreview(instructionId));
        Command command = commandDao.findByInstructionId(instructionId);
        if (command == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(command);
    }

    private static String getPreview(String value) {
        if (value == null) {
            return "";
        }
        int length = value.length();
        if (length <= 8) {
            return value;
        }
        return value.substring(0, 4) + "..." + value.substring(length - 4);
    }
}
