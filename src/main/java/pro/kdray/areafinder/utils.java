package pro.kdray.areafinder;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.bukkit.selections.Polygonal2DSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
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


    public static Selection find(Player player) throws InvalidRegionException{
        List<Location> blocks = new ArrayList<>();
        blocks = loopBlock(getBottom(player.getLocation()).getBlock(), blocks); //Find all blocks above the floor
        blocks = removeMiddle(blocks); //Find all air blocks that are touching a non air block on at least one side
        blocks = removeMiddleWalls(blocks); //Remove the blocks between each corner
        blocks = addInnerCorners(blocks); //Add in blocks to that list that are touching blocks on that list on either the west and north side or east and west (so on and so forth)
        List<Corner> corners = toCorners(blocks); //Converts the locations to corners
        corners = removeIrrelevent(corners); //Take those same blocks and remove the ones that have a side block on only the north and south side or east and west these are the corners
        corners = corners.get(0).orderCorners(corners); //Puts corners in order so it makes a proper polygon
        int bottom = (int) corners.get(0).getY(); //This should be fine *Knocks on wood*
        int top = getSmallestHight(corners)+bottom; //Check the distance between all the corners and the ceiling, the lowest value is how tall the region will be
        return new Polygonal2DSelection(player.getWorld(),locationsToVectors(corners),top,bottom); //Make the region
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
        while (working.getBlock().getType() == Material.AIR && working.getY() != 256){
            working.add(0,1,0);
        }
        return working.add(0,-1,0); //It found the top, now I have to subtract one block
    }

    public static List<BlockVector2D> locationsToVectors(List<Corner> locations){
        List<BlockVector2D> blocks = new ArrayList<>(locations.size());
        for (Location block:locations){
            blocks.add(new BlockVector2D(block.getX(),block.getZ()));
        }
        return blocks;
    }

    private static List<Location> loopBlock(Block block, List<Location> blocks) throws InvalidRegionException{
        if (block.getType() != Material.AIR
                && block.getType() != Material.ACACIA_DOOR
                && block.getType() != Material.BIRCH_DOOR
                && block.getType() != Material.DARK_OAK_DOOR
                && block.getType() != Material.JUNGLE_DOOR
                && block.getType() != Material.SPRUCE_DOOR
                && block.getType() != Material.WOOD_DOOR
                && block.getType() != Material.WOODEN_DOOR)
            return new ArrayList<>();
        if (blocks.size() > 2000) //Stop if the size is over 2 thousand blocks on the xz axis
            throw new InvalidRegionException();
        List<Location> newBlocks = new ArrayList<>();
        newBlocks.add(block.getLocation());
        if (block.getType() != Material.AIR)
            return newBlocks;
        for(BlockFace face:faces){
            Location newLocation = getRelative(block.getLocation(),face);
            if (!isLocationLoaded(newLocation))
                throw new InvalidRegionException(); //You went too far if it's unloaded. If you reach this normally, please message me I want to see that room.
            Block newBlock = newLocation.getBlock();
            List<Location> allLocations = new ArrayList<>();
            allLocations.addAll(blocks);
            allLocations.addAll(newBlocks);
            if (!allLocations.contains(newBlock.getLocation())) {
                newBlocks.addAll(loopBlock(newBlock, allLocations));
            }
        }
        return newBlocks;
    }

    private static List<Location> removeMiddle(List<Location> blocks){
        List<Location> wallBlocks = new ArrayList<>();
        for(Location block:blocks){
            boolean hasSide = false;
            Block block1 = block.getBlock();
            for(BlockFace face:faces){
                Block newBlock = block1.getRelative(face);
                if (newBlock.getType() != Material.AIR) {
                    hasSide = true;
                    break;
                }
            }
            if (hasSide){
                wallBlocks.add(block);
            }
        }
        return wallBlocks;
    }

    private static List<Location> addInnerCorners(Collection<Location> blocks) {
        List<Location> cornerBlocks = new ArrayList<>();
        cornerBlocks.addAll(blocks);
        for(Location tempblock:blocks){
            for (Location block:getSurrondingBlocks(tempblock)) {
                if (cornerBlocks.contains(block))
                    continue;
                Location northBlock = getRelative(block,BlockFace.NORTH);
                Location southBlock = getRelative(block,BlockFace.SOUTH);
                Location eastBlock = getRelative(block,BlockFace.EAST);
                Location westBlock = getRelative(block,BlockFace.WEST);
                boolean north = blocks.contains(northBlock);
                boolean south = blocks.contains(southBlock);
                boolean east = blocks.contains(eastBlock);
                boolean west = blocks.contains(westBlock);
                if (((north && east) && !(south || west))) {
                    cornerBlocks.add(block);
                } else if (((north && west) && !(south || east))) {
                    cornerBlocks.add(block);
                } else if (((south && west) && !(north || east))) {
                    cornerBlocks.add(block);
                } else if (((south && east) && !(north || west))) {
                    cornerBlocks.add(block);
                }
            }
        }
        return cornerBlocks;
    }

    private static List<Location> removeMiddleWalls(List<Location> blocks) {
        List<Location> cornerBlocks = new ArrayList<>();
        for(Location block:blocks){
            Block block1 = block.getBlock();
            boolean north = blocks.contains(block1.getRelative(BlockFace.NORTH).getLocation());
            boolean south = blocks.contains(block1.getRelative(BlockFace.SOUTH).getLocation());
            boolean east = blocks.contains(block1.getRelative(BlockFace.EAST).getLocation());
            boolean west = blocks.contains(block1.getRelative(BlockFace.WEST).getLocation());
            if ((!((north&&south)&&!(east||west))&&(!((east&&west)&&!(north||south))))){
                cornerBlocks.add(block);
            }
        }
        return cornerBlocks;
    }

    private static int getSmallestHight(List<Corner> blocks) {
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

    private static List<Location> getSurrondingBlocks(Location tempblock) {
        List<Location> locations = new ArrayList<>();
        Block block = tempblock.getBlock();
        for(BlockFace face:faces) {
            Block newBlock = block.getRelative(face);
            if (newBlock.getType() == Material.AIR) {
                locations.add(newBlock.getLocation());
            }
        }
        return locations;
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

    private static boolean isLocationLoaded(Location location){
        World world = location.getWorld();
        return world.isChunkLoaded((int) location.getX() >> 4,(int) location.getZ() >> 4); //Byte shifting magic by @SupremeMortal#7120
    }

    private static List<Corner> toCorners(List<Location> locations){
        List<Corner> corners = new ArrayList<>();
        for(Location location:locations){
            corners.add(new Corner(location));
        }
        return corners;
    }

    private static List<Corner> removeIrrelevent(List<Corner> corners){
        List<Corner> newCorners = new ArrayList<>();
        for(Corner corner:corners){
            if (corner.isRelevent(corners)){
                newCorners.add(corner);
            }
        }
        return newCorners;
    }
}
