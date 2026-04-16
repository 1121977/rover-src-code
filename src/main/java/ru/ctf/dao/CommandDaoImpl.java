package ru.ctf.dao;

import java.util.HashMap;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import ru.ctf.model.Command;

public class CommandDaoImpl extends DaoImpl<Command> implements CommandDao {

    public CommandDaoImpl() {
        super(Command.class);
    }

    public long save(Command command) {
        try (EntityManager em = localContainerEntityManagerFactoryBean.createNativeEntityManager(new HashMap<>())) {
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            em.persist(command);
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return command.getId();
    }

    public List<Command> findAll() {
        try (EntityManager em = localContainerEntityManagerFactoryBean.createNativeEntityManager(new HashMap<>())) {
            TypedQuery<Command> allCommandQuery = em.createQuery("select c from Command c", Command.class);
            return allCommandQuery.getResultList();
        } catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Command findById(Long id) {
        try (EntityManager em = localContainerEntityManagerFactoryBean.createNativeEntityManager(new HashMap<>())) {
            TypedQuery<Command> commandTypedQuery = em.createQuery("select co from Command co where co.id=:id", Command.class);
            commandTypedQuery.setParameter("id", id);
            return commandTypedQuery.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public Command findByInstructionId(String instructionId) {
        try (EntityManager em = localContainerEntityManagerFactoryBean.createNativeEntityManager(new HashMap<>())) {
            TypedQuery<Command> commandTypedQuery = em.createQuery("select co from Command co where co.instructionId=:instructionId", Command.class);
            commandTypedQuery.setParameter("instructionId", instructionId);
            return commandTypedQuery.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
