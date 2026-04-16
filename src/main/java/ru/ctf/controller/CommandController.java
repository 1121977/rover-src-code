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

    private String maskString(String input) {
        if (input == null) {
            return "null";
        }
        if (input.length() <= 8) {
            return "****";
        }
        return input.substring(0, 4) + "*****" + input.substring(input.length() - 4);
    }

    @PutMapping(value = "/api/put")
    void saveCommand(ServletResponse servletResponse, @RequestBody Command command, ServletRequest servletRequest) {
        HttpServletResponse httpServletResponse = (HttpServletResponse)servletResponse;
        commandDao.save(command);
        
        logger.info("Method: PUT; Instruction: {}; InstructionID {}",
                maskString(command.getInstruction()), 
                maskString(command.getInstructionId()));
                
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
    }

    @GetMapping(value = "/api/get/{instructionId}", produces="application/json")
    Command getCommand(@PathVariable ("instructionId") String instructionId, ServletResponse servletResponse){
        HttpServletResponse httpServletResponse = (HttpServletResponse)servletResponse;
        
        logger.info("Method: GET; InstructioID: {}", maskString(instructionId));
        
        Command command = commandDao.findByInstructionId(instructionId);
        return command;
    }
}