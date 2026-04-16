package ru.ctf.controller;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.ctf.dao.CommandDao;
import ru.ctf.model.Command;

@RestController
public class CommandController {

    @Autowired
    CommandDao commandDao;

    @Autowired
    Logger logger;

    @PutMapping(value = "/api/put")
    void saveCommand(ServletResponse servletResponse, @RequestBody Command command, ServletRequest servletRequest) {
        HttpServletResponse httpServletResponse = (HttpServletResponse)servletResponse;
        if (command == null || command.getInstruction() == null || command.getInstructionId() == null) {
            httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        commandDao.save(command);
        logger.info("Method: PUT; Instruction: {}; InstructionID {}",
                maskForLog(command.getInstruction()),
                maskForLog(command.getInstructionId()));
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
    }

    @GetMapping(value = "/api/get/{instructionId}", produces="application/json")
    Command getCommand(@PathVariable ("instructionId") String instructionId, ServletResponse servletResponse){
        HttpServletResponse httpServletResponse = (HttpServletResponse)servletResponse;
        logger.info("Method: GET; InstructionID: {}", maskForLog(instructionId));
        Command command = commandDao.findByInstructionId(instructionId);
        return command;
    }

    private static String maskForLog(String value) {
        if (value == null) {
            return "null";
        }
        int len = value.length();
        if (len < 4) {
            return "*****";
        }
        return value.substring(0, 4) + "*****" + value.substring(len - 4);
    }
}
