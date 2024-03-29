package org.acme.employeescheduling.solver;

import java.time.Duration;
import java.time.LocalDateTime;
import java.lang.Math;

import org.acme.employeescheduling.domain.EmployeeSchedule;
import org.acme.employeescheduling.domain.Employee;
import org.acme.employeescheduling.domain.Availability;
import org.acme.employeescheduling.domain.AvailabilityType;
import org.acme.employeescheduling.domain.Shift;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.Joiners;

public class EmployeeSchedulingConstraintProvider implements ConstraintProvider {

    private static int getMinuteOverlap(Shift shift1, Shift shift2) {
        // The overlap of two timeslot occurs in the range common to both timeslots.
        // Both timeslots are active after the higher of their two start times,
        // and before the lower of their two end times.
        LocalDateTime shift1Start = shift1.getStart();
        LocalDateTime shift1End = shift1.getEnd();
        LocalDateTime shift2Start = shift2.getStart();
        LocalDateTime shift2End = shift2.getEnd();
        return (int) Duration.between((shift1Start.compareTo(shift2Start) > 0) ? shift1Start : shift2Start,
                (shift1End.compareTo(shift2End) < 0) ? shift1End : shift2End).toMinutes();
    }

    private static int getShiftDurationInMinutes(Shift shift) {
        return (int) Duration.between(shift.getStart(), shift.getEnd()).toMinutes();
    }

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                requiredSkill(constraintFactory),
                noOverlappingShifts(constraintFactory),
                atLeast10HoursBetweenTwoShifts(constraintFactory),
                oneShiftPerDay(constraintFactory),
                unavailableEmployeeStart(constraintFactory),
                unavailableEmployeeEnd(constraintFactory),
                desiredDayForEmployee(constraintFactory),
                undesiredDayForEmployee(constraintFactory),
                minimumShift(constraintFactory),
                lookingHours(constraintFactory),
                seniorHours(constraintFactory),
                seniorShifts(constraintFactory),
        };
    }

    Constraint requiredSkill(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .filter(shift -> !shift.getRequiredSkill().contains(shift.getEmployee().getSkillSet()))
                // .filter(shift -> !shift.getEmployee().getSkillSet().contains(shift.getRequiredSkill()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Missing required skill");
    }

    Constraint noOverlappingShifts(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Shift.class, Joiners.equal(Shift::getEmployee),
                        Joiners.overlapping(Shift::getStart, Shift::getEnd))
                .penalize(HardSoftScore.ONE_HARD,
                        EmployeeSchedulingConstraintProvider::getMinuteOverlap)
                .asConstraint("Overlapping shift");
    }

    Constraint atLeast10HoursBetweenTwoShifts(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Shift.class,
                        Joiners.equal(Shift::getEmployee),
                        Joiners.lessThanOrEqual(Shift::getEnd, Shift::getStart))
                .filter((firstShift, secondShift) -> Duration.between(firstShift.getEnd(), secondShift.getStart()).toHours() < 10)
                .penalize(HardSoftScore.ONE_HARD,
                        (firstShift, secondShift) -> {
                            int breakLength = (int) Duration.between(firstShift.getEnd(), secondShift.getStart()).toMinutes();
                            return (10 * 60) - breakLength;
                        })
                .asConstraint("At least 10 hours between 2 shifts");
    }

    Constraint oneShiftPerDay(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Shift.class, Joiners.equal(Shift::getEmployee),
                        Joiners.equal(shift -> shift.getStart().toLocalDate()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Max one shift per day");
    }

    Constraint unavailableEmployeeStart(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .join(Availability.class, Joiners.equal((Shift shift) -> shift.getStart().toLocalDate(), Availability::getDate),
                        Joiners.equal(Shift::getEmployee, Availability::getEmployee))
                .filter((shift, availability) -> availability.getAvailabilityType() == AvailabilityType.UNAVAILABLE)
                .penalize(HardSoftScore.ONE_HARD,
                        (shift, availability) -> getShiftDurationInMinutes(shift))
                .asConstraint("Unavailable employee");
    }

    Constraint unavailableEmployeeEnd(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .join(Availability.class, Joiners.equal((Shift shift) -> shift.getEnd().toLocalDate(), Availability::getDate),
                        Joiners.equal(Shift::getEmployee, Availability::getEmployee))
                .filter((shift, availability) -> availability.getAvailabilityType() == AvailabilityType.UNAVAILABLE)
                .penalize(HardSoftScore.ONE_HARD,
                        (shift, availability) -> getShiftDurationInMinutes(shift))
                .asConstraint("Unavailable employee end");
    }

    Constraint desiredDayForEmployee(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .join(Availability.class, Joiners.equal((Shift shift) -> shift.getStart().toLocalDate(), Availability::getDate),
                        Joiners.equal(Shift::getEmployee, Availability::getEmployee))
                .filter((shift, availability) -> availability.getAvailabilityType() == AvailabilityType.DESIRED)
                .reward(HardSoftScore.ONE_SOFT,
                        (shift, availability) -> getShiftDurationInMinutes(shift))
                .asConstraint("Desired day for employee");
    }

    Constraint undesiredDayForEmployee(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .join(Availability.class, Joiners.equal((Shift shift) -> shift.getStart().toLocalDate(), Availability::getDate),
                        Joiners.equal(Shift::getEmployee, Availability::getEmployee))
                .filter((shift, availability) -> availability.getAvailabilityType() == AvailabilityType.UNDESIRED)
                .penalize(HardSoftScore.ONE_SOFT,
                        (shift, availability) -> getShiftDurationInMinutes(shift))
                .asConstraint("Undesired day for employee");
    }

    Constraint minimumShift(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .groupBy(Shift::getEmployee,ConstraintCollectors.count())
                .penalize(HardSoftScore.ONE_SOFT, (employee, count) -> {
                        return (Math.abs(count - 10) * 1200);})
                // .penalize(HardSoftScore.ONE_HARD, (employee, count) -> {
                //         return (Math.abs(count - 10));})
                .asConstraint("minimum one");
    }

    Constraint lookingHours(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
        .filter((shift) -> shift.getIsWeekend() == true)
        .groupBy(Shift::getEmployee, ConstraintCollectors.count())
        .penalize(HardSoftScore.ONE_SOFT, (employee,count) -> {
                return (Math.abs(count - 1) * 3780);})
        // .penalize(HardSoftScore.ONE_HARD, (employee,count) -> {
        //         return (Math.abs(count - 2));
        // })
        .asConstraint("yuppy");

    }

    Constraint seniorHours(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .filter((shift) -> shift.getRequiredSkill() == "PGY-3, PGY-4")
                .groupBy(Shift::getEmployee,ConstraintCollectors.count(),ConstraintCollectors.sum(Shift::getHours))
                .filter((a,b,c) -> c > 150)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("senior Hours");

    }

    Constraint seniorShifts(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Shift.class)
                .filter((shift) -> shift.getRequiredSkill() == "PGY-3, PGY-4")
                .groupBy(Shift::getEmployee,ConstraintCollectors.count())
                .filter((a,b) -> b > 7)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("senior Shifts");

    }

}
