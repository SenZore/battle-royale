Create a Minecraft Paper 1.21.x plugin using Maven that implements a mini-game server with the following specifications:

Single server instance that auto-generates a new world each game round.

Each world is small (smaller than Hoplite borders) with a normal border that shrinks over time to limit chunk generation and reduce CPU load.

Game duration in overworld is max 20 minutes per round.

Nether world uses the same shrinking border logic, The End world is disabled.

Minimum players to start the game is configurable (default 3-5) via config.yml.

When a player joins, they are marked as ready and wait for enough players.

When players reach the configured minimum (over 5), a new small world is generated and the game begins with a countdown and big in-game title plus music indicating game start and events (like PvP toggle).

No PvP arena, PvP is free with kits.

Players receive random loot drops every 1-5 minutes with random items from a pool containing all normal Minecraft PvP kits (including swords, maces, UHC, enchants, etc.).

Player death results in spectating mode, no saving of game data or stats after each round.

On player kill, send a custom global death message with killer and victim names, and summon a lightning bolt strike (no damage).

After a round finishes, teleport all players to a newly generated world spawn at coordinates (X=0, Y=0, Z=0) and delete the old world.

Scoreboard displays useful real-time game status info (player count, time left, border size, loot drop countdown, kills, etc.).

Use optimal coding methods and Paper API techniques to minimize server lag and CPU usage especially by limiting chunk loading outside the shrinking border.

Provide clear configuration options in a config.yml for player minimum, world border sizes, game time, loot drop intervals, and other game mechanics.

Please generate the full Maven project structure and sample Java classes with comments demonstrating how to implement these features, using your own efficient code design and best practices for Paper plugin development on version 1.21.x.
