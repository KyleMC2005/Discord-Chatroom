
// There are no locks, meaning that multiple threads could attempt to change these values at the same time,
// which could lead to incorrect values being displayed to the user

public class OnlineCounterBad {          // Total number of online users
    private int counter = 0;

    public void incrementOnline(String out) {
        counter++;
    }

    public void decrementOnline(String out) {
        counter--;
    }

    public int getOnlineCount() {
            return counter;
    }

}



