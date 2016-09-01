package fr.free.nrw.commons.nearby;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fr.free.nrw.commons.R;

public class NearbyListFragment extends ListFragment {

    private int mImageSize;
    private boolean mItemClicked;
    private NearbyAdapter mAdapter;

    private List<Place> places;
    private LatLng mLatestLocation;
    private ProgressBar progressBar;

    private static final String TAG = "NearbyListFragment";

    public NearbyListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "NearbyListFragment created");
        View view = inflater.inflate(R.layout.fragment_nearby, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        progressBar.setMax(10);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);

        mLatestLocation = ((NearbyActivity) getActivity()).getmLatestLocation();

        getNearbyPlaces nearbyList = new getNearbyPlaces();
        nearbyList.execute();

        Log.d(TAG, "Adapter set to ListView");

    }

    private List<Place> loadAttractionsFromLocation(final LatLng curLatLng) {

        List<Place> places = NearbyPlaces.get();
        if (curLatLng != null) {
            Log.d(TAG, "Sorting places by distance...");
            Collections.sort(places,
                    new Comparator<Place>() {
                        @Override
                        public int compare(Place lhs, Place rhs) {
                            double lhsDistance = computeDistanceBetween(
                                    lhs.location, curLatLng);
                            double rhsDistance = computeDistanceBetween(
                                    rhs.location, curLatLng);
                            return (int) (lhsDistance - rhsDistance);
                        }
                    }
            );
        }

        for(int i = 0; i < 500; i++) {
            Place place = places.get(i);
            String distance = formatDistanceBetween(mLatestLocation, place.location);
            System.out.println("Sorted " + place.name + " at " + distance + " away.");
            place.setDistance(distance);
        }
        return places;
    }

    private class getNearbyPlaces extends AsyncTask<Void, Integer, List<Place>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
        }

        @Override
        protected List<Place> doInBackground(Void... params) {
            places = loadAttractionsFromLocation(mLatestLocation);
            return places;
        }

        @Override
        protected void onPostExecute(List<Place> result) {
            super.onPostExecute(result);

            mAdapter = new NearbyAdapter(getActivity(), places);

            progressBar.setVisibility(View.GONE);

            ListView listview = (ListView) getView().findViewById(R.id.listview);
            listview.setAdapter(mAdapter);

            listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                    Place place = places.get(position);
                    LatLng placeLatLng = place.location;

                    double latitude = placeLatLng.latitude;
                    double longitude = placeLatLng.longitude;

                    Log.d(TAG, "Item at position " + position + " has coords: Lat: " + latitude + " Long: " + longitude);

                    //Open map app at given position
                    Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + latitude + "," + longitude);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

                    if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(mapIntent);
                    }
                }
            });
            mAdapter.notifyDataSetChanged();
        }
    }

    private class NearbyAdapter extends ArrayAdapter<Place> {

        public List<Place> placesList;
        private Context mContext;

        public NearbyAdapter(Context context, List<Place> places) {
            super(context, R.layout.item_place, places);
            mContext = context;
            placesList = places;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            Place place = (Place) getItem(position);
            Log.d(TAG, "Place " + place.name);

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_place, parent, false);
            }

            // Lookup view for data population
            TextView tvName = (TextView) convertView.findViewById(R.id.tvName);
            TextView tvDesc = (TextView) convertView.findViewById(R.id.tvDesc);
            TextView distance = (TextView) convertView.findViewById(R.id.distance);
            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);

            String quotelessName = place.name.replaceAll("^\"|\"$", "");

            // Populate the data into the template view using the data object
            tvName.setText(quotelessName);
            tvDesc.setText(place.description);
            distance.setText(place.distance);

            //TODO: Check for description type and set Drawable image here
            //Types of desc: landmark, city, edu, event, mountain, isle
            switch(place.description) {
                case "landmark":
                    icon.setImageResource(R.drawable.icon_landmark);
                    break;
                case "city":
                    icon.setImageResource(R.drawable.icon_city);
                    break;
                default:
                    icon.setImageResource(R.drawable.empty_photo);
            }

            // Return the completed view to render on screen
            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


/*
        @Override
        public int getItemCount() {
            return placesList == null ? 0 : placesList.size();
        }

        @Override
        public void onItemClick(View view, int position) {

            if (!mItemClicked) {
                mItemClicked = true;
                View heroView = view.findViewById(android.R.id.icon);
                DetailActivity.launch(
                        getActivity(), mAdapter.mAttractionList.get(position).name, heroView);
            }

        }*/
    }

    private String formatDistanceBetween(LatLng point1, LatLng point2) {
        if (point1 == null || point2 == null) {
            return null;
        }

        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        double distance = Math.round(computeDistanceBetween(point1, point2));

        // Adjust to KM if M goes over 1000 (see javadoc of method for note
        // on only supporting metric)
        if (distance >= 1000) {
            numberFormat.setMaximumFractionDigits(1);
            return numberFormat.format(distance / 1000) + "km";
        }
        return numberFormat.format(distance) + "m";
    }

    private static double computeDistanceBetween(LatLng from, LatLng to) {
        return computeAngleBetween(from, to) * 6371009.0D;
    }

    private static double computeAngleBetween(LatLng from, LatLng to) {
        return distanceRadians(Math.toRadians(from.latitude), Math.toRadians(from.longitude), Math.toRadians(to.latitude), Math.toRadians(to.longitude));
    }


    private static double distanceRadians(double lat1, double lng1, double lat2, double lng2) {
        return arcHav(havDistance(lat1, lat2, lng1 - lng2));
    }

    private static double arcHav(double x) {
        return 2.0D * Math.asin(Math.sqrt(x));
    }

    private static double havDistance(double lat1, double lat2, double dLng) {
        return hav(lat1 - lat2) + hav(dLng) * Math.cos(lat1) * Math.cos(lat2);
    }

    private static double hav(double x) {
        double sinHalf = Math.sin(x * 0.5D);
        return sinHalf * sinHalf;
    }

}
