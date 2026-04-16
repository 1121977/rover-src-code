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

    // Метод для экранирования Log Injection (если Encode.forJava недоступен)
    private String escapeLog(String input) {
        if (input == null) return "";
        return input.replace("${", "\\${")
                    .replace("jndi:", "jndi\\:")
                    .replace("\\", "\\\\")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }

    @PutMapping(value = "/api/put")
    void saveCommand(ServletResponse servletResponse, @RequestBody Command command, ServletRequest servletRequest) {
        HttpServletResponse httpServletResponse = (HttpServletResponse)servletResponse;
        commandDao.save(command);
        
        // Уязвимость №2 ИСПРАВЛЕНА: экранирование пользовательских данных
        String instrStart = command.getInstruction().substring(0, 4);
        String instrEnd = command.getInstruction().substring(command.getInstruction().length() - 4);
        String idStart = command.getInstructionId().substring(0, 4);
        String idEnd = command.getInstructionId().substring(command.getInstructionId().length() - 4);
        
        logger.info("Method: PUT; Instruction: {}*****{}; InstructionID {}*****{}",
            escapeLog(instrStart),
            escapeLog(instrEnd),
            escapeLog(idStart),
            escapeLog(idEnd));
        
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
    }

    @GetMapping(value = "/api/get/{instructionId}", produces="application/json")
    Command getCommand(@PathVariable ("instructionId") String instructionId, ServletResponse servletResponse){
        HttpServletResponse httpServletResponse = (HttpServletResponse)servletResponse;
        
        // Уязвимость №2 ИСПРАВЛЕНА: экранирование и здесь
        String idStart = instructionId.substring(0, 4);
        String idEnd = instructionId.substring(instructionId.length() - 4);
        logger.info("Method: GET; InstructionID: {}*****{}", escapeLog(idStart), escapeLog(idEnd));
        
        Command command = commandDao.findByInstructionId(instructionId);
        return command;
    }
}