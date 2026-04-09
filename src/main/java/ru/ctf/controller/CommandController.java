package ru.ctf.controller;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
    void saveCommand(ServletResponse servletResponse, @RequestBody Command command, ServletRequest servletRequest) {
        HttpServletResponse httpServletResponse = (HttpServletResponse)servletResponse;
        commandDao.save(command);
        logger.info("Method: PUT; Instruction: {}*****{}; InstructionID {}*****{}",
                command.getInstruction().substring(0, 4), command.getInstruction().substring(command.getInstruction().length() - 4),
                command.getInstructionId().substring(0,4), command.getInstructionId().substring(command.getInstructionId().length() - 4));
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
    }

    @GetMapping(value = "/api/get/{instructionId}", produces="application/json")
    Command getCommand(@PathVariable ("instructionId") String instructionId, ServletResponse servletResponse){
        HttpServletResponse httpServletResponse = (HttpServletResponse)servletResponse;
        logger.info("Method: GET; InstructioID: {}*****{}", instructionId.substring(0, 4), instructionId.substring(instructionId.length() - 4));
        Command command = commandDao.findByInstructionId(instructionId);
        return command;
    }
}
