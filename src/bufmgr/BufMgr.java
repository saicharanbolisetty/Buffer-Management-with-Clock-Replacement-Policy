package bufmgr;

import global.GlobalConst;
import global.Minibase;
import global.Page;
import global.PageId;
import java.util.HashMap;

/**
 * <h3>Minibase Buffer Manager</h3>
 * The buffer manager reads disk pages into a main memory page as needed. The
 * collection of main memory pages (called frames) used by the buffer manager
 * for this purpose is called the buffer pool. This is just an array of Page
 * objects. The buffer manager is used by access methods, heap files, and
 * relational operators to read, write, allocate, and de-allocate pages.
 */
public class BufMgr implements GlobalConst {

	/** Actual pool of pages (can be viewed as an array of byte arrays). */
	protected Page[] bufpool;

	/** Array of descriptors, each containing the pin count, dirty status, etc\
	. */
	protected FrameDesc[] frametab;

	/** Maps current page numbers to frames; used for efficient lookups. */
	protected HashMap<Integer, Integer> pagemap;

	protected HashMap<Integer, Integer> framemap;

	/** The replacement policy to use. */
	protected Clock replacer;
	//-------------------------------------------------------------



	/**
	 * Constructs a buffer mamanger with the given settings.
	 * 
	 * @param numbufs number of buffers in the buffer pool
	 */
	public BufMgr(int numbufs) {

		bufpool = new Page[numbufs];
		frametab = new FrameDesc[numbufs];
		for (int i=0; i<frametab.length; i++)
		{
			bufpool[i] = new Page();
			frametab[i] = new FrameDesc();
		}

		replacer = new Clock(this); 
		pagemap = new HashMap<>();
		framemap = new HashMap<>();


		//throw new UnsupportedOperationException("Not implemented");
	}

	/**
	 * Allocates a set of new pages, and pins the first one in an appropriate
	 * frame in the buffer pool.
	 * 
	 * @param firstpg holds the contents of the first page
	 * @param run_size number of pages to allocate
	 * @return page id of the first new page
	 * @throws IllegalArgumentException if PIN_MEMCPY and the page is pinned
	 * @throws IllegalStateException if all pages are pinned (i.e. pool exceeded)
	 */

	public PageId newPage(Page firstpg, int run_size) {

		if (getNumUnpinned() == 0)
		{
			throw new IllegalStateException();   
		}
		else
		{
			PageId pageno = Minibase.DiskManager.allocate_page(run_size);
			Integer FrameNum = pagemap.get(pageno.pid);
			if ((FrameNum != null) && (frametab[FrameNum].pincnt > 0))
			{
				throw new IllegalArgumentException(); 
			}
			else
			{
				pinPage(pageno, firstpg, PIN_MEMCPY);
				return pageno;
			}
		}


		//throw new UnsupportedOperationException("Not implemented");
	}

	/**
	 * Deallocates a single page from disk, freeing it from the pool if needed.
	 * 
	 * @param pageno identifies the page to remove
	 * @throws IllegalArgumentException if the page is pinned
	 */
	public void freePage(PageId pageno) {

		Integer FrameNum = pagemap.get(pageno.pid);
		if ((FrameNum != null) && (frametab[FrameNum].pincnt > 0))
		{
			throw new IllegalArgumentException();
		}
		else
		{
			Minibase.DiskManager.deallocate_page(pageno);
		}




		//throw new UnsupportedOperationException("Not implemented");
	}

	/**
	 * Pins a disk page into the buffer pool. If the page is already pinned, this
	 * simply increments the pin count. Otherwise, this selects another page in
	 * the pool to replace, flushing it to disk if dirty.
	 * 
	 * @param pageno identifies the page to pin
	 * @param page holds contents of the page, either an input or output param
	 * @param skipRead PIN_MEMCPY (replace in pool); PIN_DISKIO (read the page in)
	 * @throws IllegalArgumentException if PIN_MEMCPY and the page is pinned
	 * @throws IllegalStateException if all pages are pinned (i.e. pool exceeded)
	 */
	public void pinPage(PageId pageno, Page page, boolean skipRead) {

		Integer FrameNum = pagemap.get(pageno.pid);
		if (FrameNum == null)
		{
			//System.out.println("Not in Buffer Pool");
			int framenum = replacer.pickVictim();
			if (framenum != -1)
			{
				//System.out.println("Putting in Frame: " + framenum);
				if ((frametab[framenum].state) && (frametab[framenum].dirty))
				{
					//System.out.println("Frame dirty, writing to disk");
					Minibase.DiskManager.write_page(frametab[framenum].pageno, bufpool[framenum]);
				}


				if(skipRead==PIN_DISKIO)
				{
					//System.out.println("PIN_DISKIO");
					Page diskpage = new Page();
					Minibase.DiskManager.read_page(pageno, diskpage);
					bufpool[framenum].copyPage(diskpage);
					page.setPage(bufpool[framenum]);
					frametab[framenum].pincnt++;  
					frametab[framenum].state = true; 
					frametab[framenum].pageno = new PageId(pageno.pid); 
					addToHashMap(pageno.pid, framenum);

				}
				if(skipRead==PIN_MEMCPY)
				{
					//System.out.println("PIN_MEMCPY");
					bufpool[framenum].copyPage(page);  
					page.setPage(bufpool[framenum]);
					frametab[framenum].pincnt++;  
					frametab[framenum].state = true;  
					frametab[framenum].pageno = new PageId(pageno.pid); 
					addToHashMap(pageno.pid, framenum);

				}

				if(!skipRead==PIN_DISKIO&&!skipRead==PIN_MEMCPY)
				{
					throw new IllegalArgumentException();
				}

			}
			else
			{
				throw new IllegalStateException(); 
			}
		}
		else
		{
			//System.out.println("Already in Buffer Pool");
			frametab[FrameNum].pincnt++;  
			page.setPage(bufpool[FrameNum]);
		}


		//throw new UnsupportedOperationException("Not implemented");
	}

	/**
	 * Unpins a disk page from the buffer pool, decreasing its pin count.
	 * 
	 * @param pageno identifies the page to unpin
	 * @param dirty UNPIN_DIRTY if the page was modified, UNPIN_CLEAN otherrwise
	 * @throws IllegalArgumentException if the page is not present or not pinned
	 */
	public void unpinPage(PageId pageno, boolean dirty) {

		Integer FrameNum = pagemap.get(pageno.pid);
		if ((FrameNum == null) || frametab[FrameNum].pincnt == 0)
		{
			throw new IllegalArgumentException();      
		}
		else
		{
			frametab[FrameNum].dirty = dirty;
			frametab[FrameNum].pincnt--;
			if (frametab[FrameNum].pincnt == 0)
			{
				frametab[FrameNum].index = true;
			}
		}


		//throw new UnsupportedOperationException("Not implemented");
	}

	/**
	 * Immediately writes a page in the buffer pool to disk, if dirty.
	 */
	public void flushPage(PageId pageno) {

		Integer FrameNum = pagemap.get(pageno.pid);
		if (FrameNum != null)
		{
			if (frametab[FrameNum].dirty)
			{
				Minibase.DiskManager.write_page(pageno, bufpool[FrameNum]);
			}
		}
		else
		{
			throw new IllegalArgumentException();   
		}


		//throw new UnsupportedOperationException("Not implemented");
	}

	/**
	 * Immediately writes all dirty pages in the buffer pool to disk.
	 */
	public void flushAllPages() {

		for (int i=0; i<frametab.length; i++)
		{
			if ((frametab[i].state) && (frametab[i].dirty))
			{
				flushPage(frametab[i].pageno);    
			}
		}


		//throw new UnsupportedOperationException("Not implemented");
	}

	/**
	 * Gets the total number of buffer frames.
	 */
	public int getNumBuffers() {

		return frametab.length;


		//throw new UnsupportedOperationException("Not implemented");
	}

	/**
	 * Gets the total number of unpinned buffer frames.
	 */
	public int getNumUnpinned() {

		int unpinned_count = 0;
		for (int i=0; i<frametab.length; i++)
		{
			if (frametab[i].pincnt == 0)
			{
				unpinned_count++;
			}
		}

		return unpinned_count;


		//throw new UnsupportedOperationException("Not implemented");
	}


	private void addToHashMap(Integer Page, Integer Frame)
	{
		Integer oldPage = framemap.get(Frame);
		if (oldPage != null)
		{
			pagemap.remove(oldPage);
			framemap.remove(Frame);
		}

		pagemap.put(Page, Frame);
		framemap.put(Frame, Page);
	}


} // public class BufMgr implements GlobalConst
