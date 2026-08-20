# Ziplines

Rideable ziplines for Spigot and Paper servers. Mark out two points, and the plugin strings a line
between them that players can board and ride across.

- Lines are drawn either as real blocks or as particles.
- Riders travel on a seat, or under their own velocity if you would rather keep them in control.
- Every line has its own speed, materials, sounds, trigger and exit behaviour, all editable in game.
- Deleting a line puts back whatever blocks its path replaced.
- Block paths live only while the server is up: they go in on start-up and come out again on
  shutdown, so a line that disappears from `ziplines.yml` leaves nothing behind.

## Requirements

- Java 21 or newer
- A Spigot or Paper server on API 26.2

## Installing

Drop the jar into `plugins/` and restart the server. On first start the plugin writes
`plugins/Ziplines/config.yml`; the ziplines themselves are stored alongside it in `ziplines.yml`.

A zipline drawn from blocks is put into the world when the plugin starts and taken back out when it
stops, so the world only holds a path while there is something there to ride along it. Alongside
each zipline, `ziplines.yml` records what its path replaced, which is how those blocks are put back.
If the server is killed rather than stopped, that record is used to tidy up on the next start
instead, so nothing is left stranded.

## Building a zipline

Stand where the line should begin and look along it, then:

```
/zl start skyway
```

A particle preview follows you from that point. Walk to the far end and run:

```
/zl end
```

The line is checked for clearance before it is built: it needs four blocks of open space beneath it
along its whole length, so that a rider and their seat fit through. If something is in the way, the
plugin reports where.

To ride, walk into either end of the line. Ziplines can be ridden in both directions, or set
`direction` on one to have it board from a single end only.

Any number of players can share a line by default. Set `max-riders` on one to let only so many ride
it at a time; anyone who walks up to a line that is already full is told so rather than boarded. A
line set to a single rider hands them the seat parked at the end they board from and carries it
along with them, so the seat they walked up to is the one they leave on.

## Commands

The command is `/ziplines`, aliased to `/zipline` and `/zl`.

| Command                          | Description                                                |
|----------------------------------|------------------------------------------------------------|
| `/zl start <id> [speed]`         | Start marking out a zipline from where you are stood.      |
| `/zl end`                        | Finish it where you are stood.                             |
| `/zl cancel`                     | Abandon the one you are marking out.                       |
| `/zl list`                       | List every zipline, with its world, length and settings.   |
| `/zl edit <id> <option> <value>` | Change one setting on an existing zipline.                 |
| `/zl delete <id>`                | Delete a zipline and restore the blocks its path replaced. |

Ids may use letters, digits, hyphens and underscores, up to 32 characters.

## Permissions

| Permission        | Default  | Description                                 |
|-------------------|----------|---------------------------------------------|
| `ziplines.use`    | everyone | Ride a zipline.                             |
| `ziplines.start`  | op       | Begin marking out a zipline.                |
| `ziplines.end`    | op       | Finish marking out a zipline.               |
| `ziplines.edit`   | op       | Change the settings of an existing zipline. |
| `ziplines.delete` | op       | Delete a zipline.                           |
| `ziplines.list`   | op       | List the ziplines on the server.            |
| `ziplines.admin`  | op       | All of the above.                           |

## Settings

Every setting below can be edited per zipline with `/zl edit`, and defaulted for new ziplines under
`defaults` in `config.yml`. Tab completion offers the valid values for each one.

| Option                   | Values                                 | Description                                                    |
|--------------------------|----------------------------------------|----------------------------------------------------------------|
| `speed`                  | 0.01 – 10                              | Ride speed multiplier. 1.0 is about 8 blocks per second.       |
| `path-type`              | `BLOCK`, `PARTICLE`                    | Whether the line is built from blocks or drawn with particles. |
| `material`               | any block                              | Block the line is built from.                                  |
| `path-particle`          | any plain particle                     | Particle the line is drawn with.                               |
| `endpoint-particle`      | any plain particle                     | Particle circling each end that can be boarded.                |
| `trigger`                | `WALK`, `RIGHT_CLICK`                  | What a player does to board.                                   |
| `direction`              | `BOTH`, `START_TO_END`, `END_TO_START` | Which ends the line can be boarded from.                       |
| `movement-mode`          | `MOUNTED`, `VELOCITY`                  | Whether riders sit on a seat or are pushed along.              |
| `exit-mode`              | `DROP`, `LAUNCH`                       | What happens at the far end.                                   |
| `launch-power`           | 0 – 10                                 | Strength of the throw when `exit-mode` is `LAUNCH`.            |
| `max-riders`             | 1 – 100, or `NONE`                     | How many players may ride the line at once.                    |
| `ride-sound`             | any sound, or `NONE`                   | Sound played while riding.                                     |
| `ride-sound-volume`      | 0 – 2                                  |                                                                |
| `ride-sound-interval`    | 1 – 100                                | Ticks between repeats of the ride sound.                       |
| `ride-sound-pitch-start` | 0.5 – 2                                | Pitch at the start of the ride.                                |
| `ride-sound-pitch-end`   | 0.5 – 2                                | Pitch at the end, slid to across the ride.                     |
| `end-sound`              | any sound, or `NONE`                   | Sound played on reaching the end.                              |
| `end-sound-volume`       | 0 – 2                                  |                                                                |
| `end-sound-pitch`        | 0.5 – 2                                |                                                                |
| `seat`                   | `true`, `false`                        | Show a seat under the rider and at each boardable end.         |
| `seat-material`          | any block                              | Block the seat is made of. Slabs sit best.                     |
| `seat-scale`             | 0.05 – 2                               | Size of the seat block.                                        |
| `seat-offset`            | -2 – 2                                 | Nudge the seat up or down.                                     |
| `fall-damage`            | `true`, `false`                        | Take fall damage from the drop at the end.                     |
| `sneak-exit`             | `true`, `false`                        | Let riders leave the line early by sneaking.                   |

Two further settings are server wide rather than per zipline:

| Option           | Default | Description                                                      |
|------------------|---------|------------------------------------------------------------------|
| `trigger-radius` | 1.5     | How close to an endpoint a player has to be to board, in blocks. |
| `max-length`     | 256     | Longest zipline that may be built, in blocks.                    |

Sounds are named the way Minecraft's own constants are, such as `BLOCK_NOTE_BLOCK_BASS`. Namespaced
keys like `minecraft:block.note_block.bass` are accepted too.

## Building from source

```
mvn clean package
```

The jar lands in `target/Ziplines-<version>.jar`. Tests run as part of the build:

```
mvn test
```

## Licence

[MIT](LICENSE).
