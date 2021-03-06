import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uchicago.src.reflector.RangePropertyDescriptor;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.DisplaySurface;

/**
 * Class that implements the simulation model for the rabbits grass simulation.
 * This is the first class which needs to be setup in order to run Repast
 * simulation. It manages the entire RePast environment and the simulation.
 *
 * @author
 */

public class RabbitsGrassSimulationModel extends SimModelImpl {
	/**
	 * Prepares the model for a new run
	 */
	@Override
	public void setup() {
		// clean everything in fact
		space = null;
		surface = null;
		schedule = null;
		rabbits = null;

		// register sliders
		for (String parameter : PARAMETERS)
			registerSlider(parameter);
	}

	/**
	 * Gets the names of the initial model parameters to set.
	 */
	@Override
	public String[] getInitParam() {
		return PARAMETERS;
	}

	/**
	 * Begins a simulation run.
	 */
	@Override
	public void begin() {
		buildModel();
		buildSchedule();
		buildDisplay();

		// Open the display window
		surface.display();
	}

	@Override
	public String getName() {
		return "Killer Rabbit of Caerbannog";
	}

	@Override
	public Schedule getSchedule() {
		return schedule;
	}

	public int getGridSize() {
		return gridSize;
	}

	public void setGridSize(int gridSize) {
		this.gridSize = gridSize;
	}

	public int getGrassGrowthRate() {
		return grassGrowthRate;
	}

	public void setGrassGrowthRate(int grassGrowthRate) {
		this.grassGrowthRate = grassGrowthRate;
	}

	public int getInitialRabbits() {
		return initialRabbits;
	}

	public void setInitialRabbits(int initialRabbits) {
		this.initialRabbits = initialRabbits;
	}

	public int getBirthThreshold() {
		return birthThreshold;
	}

	public void setBirthThreshold(int birthThreshold) {
		this.birthThreshold = birthThreshold;
	}

	public int getMaxEatQuantity() {
		return maxEatQuantity;
	}

	public void setMaxEatQuantity(int maxEatQuantity) {
		this.maxEatQuantity = maxEatQuantity;
	}

	public int getInitialAgentEnergy() {
		return initialAgentEnergy;
	}

	public void setInitialAgentEnergy(int initialAgentEnergy) {
		this.initialAgentEnergy = initialAgentEnergy;
	}

	public int getMoveEnergyCost() {
		return moveEnergyCost;
	}

	public void setMoveEnergyCost(int moveEnergyCost) {
		this.moveEnergyCost = moveEnergyCost;
	}

	public int getInitialAmountOfGrass() {
		return initialAmountOfGrass;
	}

	public void setInitialAmountOfGrass(int initialAmountOfGrass) {
		this.initialAmountOfGrass = initialAmountOfGrass;
	}

	public int getEnergyConsumptionRate() {
		return energyConsumptionRate;
	}

	public void setEnergyConsumptionRate(int energyConsumptionRate) {
		this.energyConsumptionRate = energyConsumptionRate;
	}

	private void buildModel() {
		int size = getGridSize();

		space = new RabbitsGrassSimulationSpace(size);
		space.growGrass(getInitialAmountOfGrass());

		// Insert at most size x size rabbits on the plane
		rabbits = new ArrayList<RabbitsGrassSimulationAgent>();
		int rabbitCount = getInitialRabbits();
		rabbitCount = Math.min(rabbitCount, size * size);
		while (rabbitCount > 0) {
			int x = Utils.uniform(0, size - 1);
			int y = Utils.uniform(0, size - 1);
			if (space.isFreeForRabbit(x, y)) {
				RabbitsGrassSimulationAgent agent = new RabbitsGrassSimulationAgent(
						x, y, getInitialAgentEnergy(), space);
				rabbits.add(agent);
				rabbitCount--;
			}
		}
		createChart("Populations Evolution");

	}

	private void buildSchedule() {
		schedule = new Schedule();

		// Perform all actions in one event at every clock tick
		schedule.scheduleActionAtInterval(1, new BasicAction() {
			@Override
			public void execute() {
				space.growGrass(grassGrowthRate);
				updateRabits();
				surface.updateDisplay();
				popGraph.step();
			}
		});
	}

	private void buildDisplay() {
		surface = new DisplaySurface(space.getDimension(), this, "Display");
		surface.addDisplayable(space.getGrassDisplayable(), "Grass");
		surface.addDisplayable(space.getRabbitsDisplayable(), "Rabbits");

		registerDisplaySurface("World", surface);
		popGraph.display();
	}

	private void updateRabits() {
		// Shuffle the rabbits for better simulation
		Collections.shuffle(rabbits);

		List<RabbitsGrassSimulationAgent> offsprings = new ArrayList<RabbitsGrassSimulationAgent>();

		// Update agents and remove the dead ones
		for (int i = 0; i < rabbits.size();) {
			RabbitsGrassSimulationAgent agent = rabbits.get(i);

			RabbitsGrassSimulationAgent offspring = agent.step(
					getMaxEatQuantity(), getMoveEnergyCost(),
					getInitialAgentEnergy(), getBirthThreshold(),
					getEnergyConsumptionRate());

			if (offspring != null) {
				offsprings.add(offspring);
			}

			if (agent.isDead()) {
				rabbits.remove(i); // was removed from the space already
			} else {
				i++;
			}
		}

		// Integrate new generation of agents into the current population
		rabbits.addAll(offsprings);
	}

	private void registerSlider(String parameter) {
		RangePropertyDescriptor slider = new RangePropertyDescriptor(parameter,
				0, 1000, 200);
		descriptors.put(parameter, slider);
	}

	private void createChart(String graphName) {
		popGraph = new OpenSequenceGraph(graphName, this);
		popGraph.setXRange(0, 200);
		popGraph.setYRange(0, 5);
		popGraph.setAxisTitles("time", "log Num. of population");

		// Y axis is Log scale due to the difference between Grass and rabbits
		popGraph.addSequence("Num. Rabbits", new Sequence() {

			@Override
			public double getSValue() {
				return Math.log10((double) rabbits.size());
			}
		});
		popGraph.addSequence("Num. Grass", new Sequence() {

			@Override
			public double getSValue() {
				return Math.log10((double) space.getGrassPopulation());
			}
		});

	}

	// Our even scheduler
	private Schedule schedule;

	// Our space representation
	private RabbitsGrassSimulationSpace space;

	// 2D surface for rendering
	private DisplaySurface surface;

	// Our collection of agents
	private List<RabbitsGrassSimulationAgent> rabbits;

	// Population chart
	private OpenSequenceGraph popGraph;

	// Simulation parameters
	private int gridSize = DEFAULT_GRID_SIZE;
	private int grassGrowthRate = DEFAULT_GRASS_GROWTH_RATE;
	private int initialRabbits = DEFAULT_INIITIAL_RABBITS;
	private int birthThreshold = DEFAULT_BRITH_THRESHOLD;
	private int maxEatQuantity = DEFAULT_MAX_EAT_QUANTITY;
	private int initialAgentEnergy = DEFAULT_INITIAL_ARGENT_ENERGY;
	private int moveEnergyCost = DEFAULT_MOVE_ENERGY_CAST;
	private int initialAmountOfGrass = DEFAULT_INITIAL_AMOUNT_OF_GRASS;
	private int energyConsumptionRate = DEFAULT_ENERGY_CONSUMPTION_RATE;

	// Default values for parameters
	static private final int DEFAULT_GRID_SIZE = 20;
	static private final int DEFAULT_GRASS_GROWTH_RATE = 120;
	static private final int DEFAULT_INIITIAL_RABBITS = 5;
	static private final int DEFAULT_BRITH_THRESHOLD = 190;
	static private final int DEFAULT_MAX_EAT_QUANTITY = 50;
	static private final int DEFAULT_INITIAL_ARGENT_ENERGY = 30;
	static private final int DEFAULT_MOVE_ENERGY_CAST = 2;
	static private final int DEFAULT_INITIAL_AMOUNT_OF_GRASS = 700;
	static private final int DEFAULT_ENERGY_CONSUMPTION_RATE = 2;

	static private final String[] PARAMETERS = new String[] { "GridSize",
			"GrassGrowthRate", "InitialRabbits", "BirthThreshold",
			"MaxEatQuantity", "InitialAgentEnergy", "MoveEnergyCost",
			"InitialAmountOfGrass", "EnergyConsumptionRate" };
}
