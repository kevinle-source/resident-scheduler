package org.acme.employeescheduling.domain;

import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;

import javax.persistence.Column;

import org.optaplanner.core.api.domain.lookup.PlanningId;

@Entity
public class Employee {
    @Id
    @PlanningId
    String name;

    // @ElementCollection(fetch = FetchType.EAGER)
    // Set<String> skillSet;

    @Column
    String skillSet;

    @Column
    int numShifts;

    public Employee() {

    }

    // public Employee(String name, Set<String> skillSet) {
    //     this.name = name;
    //     this.skillSet = skillSet;
    // }

    public Employee(String name, String skillSet) {
        this.name = name;
        this.skillSet = skillSet;
        this.numShifts = 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // public Set<String> getSkillSet() {
    //     return skillSet;
    // }

    public String getSkillSet() {
        return skillSet;
    }

    // public int getNumShifts() {
    //     return numShifts;
    // }

    // public void setSkillSet(Set<String> skillSet) {
    //     this.skillSet = skillSet;
    // }

    public void setSkillSet(String skillSet) {
        this.skillSet = skillSet;
    }

    // public void addNumShifts() {
    //     this.numShifts++;
    // }

    @Override
    public String toString() {
        return name;
    }
}
