class InsufficientStockException extends Exception {
    InsufficientStockException(String msg) {
        super(msg);
    }
}

class ERP {
    static void checkStock(int stock, int required)
            throws InsufficientStockException {

        if (required > stock)
            throw new InsufficientStockException(
                "Insufficient Stock!");
        
        System.out.println("Order Accepted");
    }

    public static void main(String[] args) {
        try {
            checkStock(10, 15);
        }
        catch (InsufficientStockException e) {
            System.out.println(e.getMessage());
        }
    }
}