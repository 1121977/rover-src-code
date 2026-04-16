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
        
        String instruction = command.getInstruction() != null ? command.getInstruction() : "";
        String instructionId = command.getInstructionId() != null ? command.getInstructionId() : "";
        
        String logInstruction = instruction.length() >= 8 ? 
                instruction.substring(0, 4) + "*****" + instruction.substring(instruction.length() - 4) : 
                "********";
        String logInstructionId = instructionId.length() >= 8 ? 
                instructionId.substring(0, 4) + "*****" + instructionId.substring(instructionId.length() - 4) : 
                "********";

        logger.info("Method: PUT; Instruction: {}; InstructionID {}", logInstruction, logInstructionId);
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
    }

    @GetMapping(value = "/api/get/{instructionId}", produces="application/json")
    Command getCommand(@PathVariable ("instructionId") String instructionId, ServletResponse servletResponse){
        HttpServletResponse httpServletResponse = (HttpServletResponse)servletResponse;
        
        String logInstructionId = (instructionId != null && instructionId.length() >= 8) ? 
                instructionId.substring(0, 4) + "*****" + instructionId.substring(instructionId.length() - 4) : 
                "********";
        
        logger.info("Method: GET; InstructionID: {}", logInstructionId);
        Command command = commandDao.findByInstructionId(instructionId);
        return command;
    }
}
