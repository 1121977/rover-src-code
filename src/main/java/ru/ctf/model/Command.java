package ru.ctf.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Date;
import java.util.Objects;

@Entity
public class Command {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;
    private String instruction;
    private Date commandDateTime;
    private String instructionId;

    public Command() {
        this.commandDateTime = new Date();
    }

    public Command(Long id, String instruction, Date commandDateTime, String instructionId) {
        this.id = id;
        this.instruction = instruction;
        this.commandDateTime = commandDateTime;
        this.instructionId = instructionId;
    }

    public Command(Long id, String instruction, String instructionId){
        this(id, instruction, new Date(), instructionId);
    }

    public Command(String instruction, String instructionId){
        this();
        this.instruction = instruction;
        this.instructionId = instructionId;
        this.commandDateTime = new Date();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public Date getCommandDateTime() {
        return commandDateTime;
    }

    public void setCommandDateTime(Date commandDateTime) {
        this.commandDateTime = commandDateTime;
    }

    public String getInstructionId() {
        return instructionId;
    }

    public void setInstructionId(String instructionId) {
        this.instructionId = instructionId;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Command command)) return false;
        return Objects.equals(instructionId, command.instructionId) && Objects.equals(id, command.id) && Objects.equals(instruction, command.instruction) && Objects.equals(commandDateTime, command.commandDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, instruction, commandDateTime, instructionId);
    }
}
