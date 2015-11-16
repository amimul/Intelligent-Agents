package planner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.topology.Topology.City;
import planner.Action.Event;

/**
 * Define plans for the vehicle of one agent
 */
public class GeneralPlan {
	private final Map<Vehicle, List<Action>> plans; // One plan per vehicle

	// Keep track of the vehicle order to construct a logist plan
	private final List<Vehicle> vehicles;

	private double overallCache = -1; // for lazy evaluation
	private List<Plan> logistPlansCache = null; // for lazy evaluation

	// NOTE: if needed, for optimization purpose only, we can create a class for immutable Vehicle Plan in order to
	// cache plans' cost: that way, when some vehicles' plans are not changed we don't have to recompute things we
	// already know about.

	public GeneralPlan(Map<Vehicle, List<Action>> plans, List<Vehicle> vehicles) {
		this.plans = plans;
		this.vehicles = vehicles;
	}

	public double computeCost() {
		// Return the cost if we already know it, otherwise we compute it
		if (overallCache >= 0)
			return overallCache;

		overallCache = 0;
		for (Vehicle vehicle : vehicles) {
			List<Action> plan = plans.get(vehicle);
			overallCache += computeCost(vehicle, plan);
		}

		return overallCache;
	}

	private double computeCost(Vehicle vehicle, List<Action> actions) {
		double cost = 0;
		City currentCity = vehicle.getCurrentCity();

		for (Action action : actions) {
			City nextCity = null;
			if (action.event == Event.PICK) {
				nextCity = action.task.pickupCity;
			} else {
				nextCity = action.task.deliveryCity;
			}

			double distance = currentCity.distanceTo(nextCity);
			currentCity = nextCity;

			cost += distance * vehicle.costPerKm();
		}

		return cost;
	}

	public List<Plan> convertToLogistPlans() {
		if (logistPlansCache != null)
			return logistPlansCache;

		// Keep the correct order for plan
		logistPlansCache = new ArrayList<>(vehicles.size());
		for (Vehicle vehicle : vehicles) {
			List<Action> plan = plans.get(vehicle);
			Plan logistPlan = convertToLogistPlan(vehicle, plan);
			logistPlansCache.add(logistPlan);
		}

		return logistPlansCache;
	}

	private Plan convertToLogistPlan(Vehicle vehicle, List<Action> actions) {
		City currentCity = vehicle.getCurrentCity();
		Plan logistPlan = new Plan(currentCity);

		for (Action action : actions) {
			if (action.event == Event.PICK) {
				// move to pickup location & pick it up
				for (City city : currentCity.pathTo(action.task.pickupCity)) {
					logistPlan.appendMove(city);
				}

				logistPlan.appendPickup(action.task);
				currentCity = action.task.pickupCity;
			} else {
				// move to delivery location & deliver
				for (City city : currentCity.pathTo(action.task.deliveryCity)) {
					logistPlan.appendMove(city);
				}

				logistPlan.appendDelivery(action.task);
				currentCity = action.task.deliveryCity;
			}
		}

		return logistPlan;
	}
}