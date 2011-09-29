package tappem.tappats;


import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AppStatusEfficientAdapter extends BaseAdapter {
	private LayoutInflater mInflater;
	private String txt;
	private final int minListItems = 3;
	private Context context;
	private int resource;
	public AppStatusEfficientAdapter(Context context, String txt, int layoutResource) {
		mInflater = LayoutInflater.from(context);
		this.txt = txt;
		this.context = context;
		resource = layoutResource;

	}

	public int getCount() {

		
			return minListItems;
		
	}

	public Object getItem(int position) {
		return position;
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(resource, null);
			holder = new ViewHolder();
			holder.statusText = (TextView) convertView.findViewById(R.id.appStatus);
			
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		try{
			
			if(position == 1)

				holder.statusText.setText(txt);




			
		}catch(Exception e)
		{
			
			holder.statusText.setText("");
		}
		return convertView;
	}

	static class ViewHolder {
		
		TextView statusText;
		
	}
}
