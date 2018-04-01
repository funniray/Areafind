package pro.kdray.areafinder;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * Created by funniray on 3/31/18.
 */
public class Corner extends Location {

    private List<BlockFace> usedSides = new ArrayList<>();
    private boolean start;
    int distance;

    private final static Collection<BlockFace> faces;
    static {
        List<BlockFace> tempList = new ArrayList<>(4);
        tempList.add(BlockFace.NORTH);
        tempList.add(BlockFace.SOUTH);
        tempList.add(BlockFace.EAST);
        tempList.add(BlockFace.WEST);
        faces = Collections.unmodifiableCollection(tempList);
    }

    public Corner(Location location) {
        super(location.getWorld(), location.getX(), location.getY(), location.getZ());
        start = false;
    }

    public boolean isRelevent(Collection<Corner> corners){
        int sameX = -1;
        int sameZ = -1;
        for(Corner corner:corners){
            if (this.getX() == corner.getX()){
                sameX++;
            }
            if (this.getZ() == corner.getZ()){
                sameZ++;
            }
        }
        return !((sameX>=2&&sameZ==0)||(sameZ>=2&&sameX==0));
    }

    public Corner getCornerOnSide(List<Corner> corners, BlockFace face){
        int closestDistance = 0;
        Corner closestCorner = null;
        for (Corner corner:corners){
            switch(face){
                case NORTH:
                    if (corner.getZ() > this.getZ() && corner.getX()==this.getX()){
                        int distance = (int) Math.abs(corner.getZ()-this.getZ());
                        if (closestDistance == 0 || distance < closestDistance){
                            closestDistance = distance;
                            closestCorner = corner;
                        }
                    }
                    break;
                case SOUTH:
                    if (corner.getZ() < this.getZ() && corner.getX()==this.getX()){
                        int distance = (int) Math.abs(corner.getZ()-this.getZ());
                        if (closestDistance == 0 || distance < closestDistance){
                            closestDistance = distance;
                            closestCorner = corner;
                        }
                    }
                    break;
                case EAST:
                    if (corner.getX() > this.getX() && corner.getZ()==this.getZ()){
                        int distance = (int) Math.abs(corner.getX()-this.getX());
                        if (closestDistance == 0 || distance < closestDistance){
                            closestDistance = distance;
                            closestCorner = corner;
                        }
                    }
                    break;
                case WEST:
                    if (corner.getX() < this.getX() && corner.getZ()==this.getZ()){
                        int distance = (int) Math.abs(corner.getX()-this.getX());
                        if (closestDistance == 0 || distance < closestDistance){
                            closestDistance = distance;
                            closestCorner = corner;
                        }
                    }
                    break;
            }
        }
        if (closestCorner != null)
            closestCorner.distance = closestDistance;
        return closestCorner;
    }

    public List<Corner> orderCorners(List<Corner> corners) throws InvalidRegionException {
        try {
            return orderCorners(corners, null, new ArrayList<>());
        }catch(StackOverflowError e){
            throw new InvalidRegionException();
        }
    }

    private List<Corner> orderCorners(List<Corner> corners, BlockFace faceFrom, List<Corner> alreadyThere){
        List<Corner> newCorners = new ArrayList<>();
        if (faceFrom == null){
            start = true;
        }else{
            usedSides.add(faceFrom);
            if (start){
                return corners;
            }
        }
        if (!alreadyThere.contains(this))
            newCorners.add(this);

        Corner closestCorner = null;
        BlockFace closestFace = null;
        boolean secondTry = false;

        for(BlockFace face:faces){
            if (usedSides.contains(face))
                continue;
            Corner corner = this.getCornerOnSide(corners,face);
            if (corner!=null){
                if (closestCorner == null){
                    closestCorner = corner;
                    closestFace = face;
                }else if (closestCorner.distance > corner.distance){
                    closestCorner = corner;
                    closestFace = face;
                }
            }
        }
        if (closestCorner == null){
            Corner corner = this.getCornerOnSide(corners,faceFrom);
            if (corner!=null){
                closestCorner = corner;
                closestFace = faceFrom;
                secondTry = true;
            }
        }

        if (closestCorner == null){
            return newCorners;
        }


        if (secondTry){
            List<Corner> withoutThat = new ArrayList<>();
            withoutThat.addAll(newCorners);
            withoutThat.remove(closestCorner);
            newCorners.addAll(closestCorner.orderCorners(corners,closestFace.getOppositeFace(),withoutThat));
            alreadyThere.remove(closestCorner);
            newCorners.removeAll(alreadyThere);
        }else {
            newCorners.addAll(closestCorner.orderCorners(corners, closestFace.getOppositeFace(), newCorners));
            newCorners.removeAll(alreadyThere);
        }
        return newCorners;
    }
}
