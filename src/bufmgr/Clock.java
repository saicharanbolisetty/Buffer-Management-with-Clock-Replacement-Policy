package bufmgr;

/**
 * The "Clock" replacement policy.
 */
class Clock extends Replacer {

	//
	// Frame State Constants
	//
	protected static final int AVAILABLE = 10;
	protected static final int REFERENCED = 11;
	protected static final int PINNED = 12;

	/** Clock head; required for the default clock algorithm. */
	protected int head;

	// --------------------------------------------------------------------------

	/**
	 * Constructs a clock replacer.
	 */
	public Clock(BufMgr bufmgr) {
		super(bufmgr);

		// initialize the clock head
		head = 0;

	} // public Clock(BufMgr bufmgr)

	/**
	 * Notifies the replacer of a new page.
	 */
	public void newPage(FrameDesc fdesc) {
		// no need to update frame state
	}

	/**
	 * Notifies the replacer of a free page.
	 */
	public void freePage(FrameDesc fdesc) {

	}

	/**
	 * Notifies the replacer of a pined page.
	 */
	public void pinPage(FrameDesc fdesc) {

	}

	/**
	 * Notifies the replacer of an unpinned page.
	 */
	public void unpinPage(FrameDesc fdesc) {

	}

	/**
	 * Selects the best frame to use for pinning a new page.
	 * 
	 * @return victim frame number, or -1 if none available
	 */
	public int pickVictim() {

		boolean found = false;

		for (int i=0; i<(frametab.length*2); i++)
		{
			if (!frametab[head].state)
			{
				found = true;
				break;
			}
			else if (frametab[head].pincnt == 0)
			{
				if (!frametab[head].index)
				{
					found = true;
					break;
				}
				else
				{
					frametab[head].index = false;                 
				}
			}

			head = (head + 1) % frametab.length;
		}

		if (found)
		{
			return head;
		}
		else
		{
			return -1;
		}



		// keep track of the number of tries

		// return the victim page


	} // public int pick_victim()

} // class Clock extends Replacer
