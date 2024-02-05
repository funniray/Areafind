package pro.kdray.areafinder;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by funniray on 3/30/18.
 */
public class utils {

    private final static Collection<BlockFace> faces;
    static {
        List<BlockFace> tempList = new ArrayList<>(4);
        tempList.add(BlockFace.NORTH);
        tempList.add(BlockFace.SOUTH);
        tempList.add(BlockFace.EAST);
        tempList.add(BlockFace.WEST);
        faces = Collections.unmodifiableCollection(tempList);
    }

    public static Polygonal2DRegionSelector find(Player player) throws InvalidRegionException{
        List<Location> blocks = getCorners(getNorth(getBottom(player.getLocation())));
        int bottom = (int) blocks.get(0).getY(); //This should be fine *Knocks on wood*
        int top = getSmallestHight(blocks)+bottom; //Check the distance between all the corners and the ceiling, the lowest value is how tall the region will be
        return new Polygonal2DRegionSelector(BukkitAdapter.adapt(player.getWorld()),locationsToVectors(blocks),top,bottom); //Make the region
    }

    public static List<Location> getCorners(Location start) throws InvalidRegionException {
        Block working = start.clone().getBlock();
        BlockFace direction = BlockFace.EAST;
        List<Location> corners = new ArrayList<>();
        Location firstCorner = null;
        boolean firstRun = true;
        int distance = 0;

        while (true){

            if (nearUnloadedChunk(working.getLocation()))
                throw new InvalidRegionException();

            BlockFace left = getLeft(direction);
            Block leftBlock = working.getRelative(left);
            Block frontBlock = working.getRelative(direction);

            if (isAir(leftBlock.getType()) || !isAir(frontBlock.getType())){
                corners.add(working.getLocation());
                if (!firstRun && firstCorner == null){
                    firstCorner = working.getLocation();
                }else if(sameLocation(firstCorner,working.getLocation())){
                    return corners;
                }
                if (isAir(leftBlock.getType())) { //If air is to your left, go left
                    direction = left;
                    working = leftBlock;
                }else if (!isAir(frontBlock.getType()) && isAir(working.getRelative(left.getOppositeFace()).getType())) { //else if there's a block in front and there's air to your right, go right
                    direction = left.getOppositeFace();
                    working = working.getRelative(direction);
                }else if (isAir(working.getRelative(direction.getOppositeFace()).getType())){ //Else if there's air behind you, go behind you
                    direction = direction.getOppositeFace();
                    working = working.getRelative(direction);
                }else {
                    throw new InvalidRegionException();
                }
            }else {
                working = frontBlock;
            }

            if (working.getType() != Material.AIR){
                corners.add(working.getLocation());
                direction = direction.getOppositeFace();
                working = working.getRelative(direction);
                if(sameLocation(firstCorner,working.getLocation())){
                    firstCorner = null;
                }
            }

            if (firstRun)
                firstRun = false;

            if(distance >= 10000) {
                throw new InvalidRegionException();
            }else{
                distance++;
            }

            if(working.getType() != Material.AIR)
                throw new InvalidRegionException();
        }
    }

    public static boolean isAir(Material type){
        return type == Material.AIR;
    }

    public static boolean sameLocation(Location location1, Location location2){
        if (location1 == null || location2 == null){
            return false;
        }
        return location1.getX() == location2.getX() && location1.getY() == location2.getY() && location1.getZ() == location2.getZ();
    }

    public static BlockFace getLeft(BlockFace origin){
        switch (origin){
            case NORTH:
                return BlockFace.WEST;
            case WEST:
                return BlockFace.SOUTH;
            case SOUTH:
                return BlockFace.EAST;
            case EAST:
                return BlockFace.NORTH;
            default:
                return null;
        }
    }

    public static Location getNorth(Location start){
        Location working = start.clone();
        while (working.getBlock().getType() == Material.AIR && working.getY() != 0){
            working.add(0,0,-1);
        }
        return working.add(0,0,1); //It found the floor, now I have to add one block
    }

    public static Location getBottom(Location start){
        Location working = start.clone();
        while (working.getBlock().getType() == Material.AIR && working.getY() != 0){
            working.add(0,-1,0);
        }
        return working.add(0,1,0); //It found the floor, now I have to add one block
    }

    public static Location getTop(Location start){
        Location working = start.clone();
        if (start.getBlock().getType() != Material.AIR)
            return working.add(0,100,0);
        while ((working.getBlock().getType() == Material.AIR || working.getBlock().getType() == Material.TORCH) && working.getY() != 256){
            working.add(0,1,0);
        }
        return working.add(0,-1,0); //It found the top, now I have to subtract one block
    }

    private static int getSmallestHight(List<Location> blocks) {
        double lowestHight = 256;
        for (Location block:blocks){
            Location top = getTop(block);
            double distance = top.getY() - block.getY();
            if (distance < lowestHight){
                lowestHight = distance;
            }
        }
        return (int) lowestHight;
    }

    private static List<BlockVector2> locationsToVectors(List<Location> blocks){
        List<BlockVector2> vectors = new ArrayList<>();
        for (Location location:blocks){
            vectors.add(BlockVector2.at(location.getX(),location.getZ()));
        }
        return vectors;
    }

    public static Location getRelative(Location location, BlockFace face){
        Location working = location.clone();
        switch (face){
            case NORTH:
                working.add(0,0,-1);
                break;
            case SOUTH:
                working.add(0,0,1);
                break;
            case EAST:
                working.add(1,0,0);
                break;
            case WEST:
                working.add(-1,0,0);
                break;
        }
        return working;
    }

    private static boolean nearUnloadedChunk(Location location){
        for (BlockFace face:faces){
            if (!isLocationLoaded(getRelative(location,face)))
                return true;
        }
        return false;
    }

    private static boolean isLocationLoaded(Location location){
        World world = location.getWorld();
        return world.isChunkLoaded((int) location.getX() >> 4,(int) location.getZ() >> 4); //Byte shifting magic by @SupremeMortal#7120
    }
}
