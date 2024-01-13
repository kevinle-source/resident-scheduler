package org.acme.employeescheduling.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

@Entity
@PlanningEntity(pinningFilter = ShiftPinningFilter.class)
public class Shift {
    @Id
    @PlanningId
    @GeneratedValue
    Long id;

    LocalDateTime start;
    @Column(name = "endDateTime") // "end" clashes with H2 syntax.
    LocalDateTime end;

    String location;
    String requiredSkill;
    int hours;
    @Column
    boolean isWeekend;
    boolean isPrimary;

    @PlanningVariable
    @ManyToOne
    Employee employee;


    public Shift() {
    }

    public Shift(LocalDateTime start, LocalDateTime end, String location, String requiredSkill,int hours, boolean isWeekend, boolean isPrimary ) {
        this(start, end, location, requiredSkill, hours, isWeekend, isPrimary, null);
    }

    public Shift(LocalDateTime start, LocalDateTime end, String location, String requiredSkill, int hours, boolean isWeekend, boolean isPrimary, Employee employee) {
        this(null, start, end, location, requiredSkill, hours, isWeekend, isPrimary, employee);

    }

    public Shift(Long id, LocalDateTime start, LocalDateTime end, String location, String requiredSkill, int hours, boolean isWeekend, boolean isPrimary, Employee employee ) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.location = location;
        this.requiredSkill = requiredSkill;
        this.hours = hours;
        this.isWeekend = isWeekend;
        this.isPrimary = isPrimary;
        this.employee = employee;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getRequiredSkill() {
        return requiredSkill;
    }

    public void setRequiredSkill(String requiredSkill) {
        this.requiredSkill = requiredSkill;
    }

    public Employee getEmployee() {
        return employee;
    }


    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int amount) {
        this.hours = amount;
    }

    public boolean getIsWeekend() {
        return isWeekend;
    }

    public boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsWeekend(boolean value) {
        this.isWeekend = value;
    }

    public void setIsPrimary(boolean value) {
        this.isPrimary = value;
    }

    @Override
    public String toString() {
        return location + " " + start + "-" + end;
    }
}
