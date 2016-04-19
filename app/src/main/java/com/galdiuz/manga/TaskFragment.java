package com.galdiuz.manga;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;

public class TaskFragment<Param, Result> extends Fragment {
    // TODO: Replace with loader?

    public interface TaskCallbacks<Param, Result> {
        void onPreExecute();
        Result doInBackground(Param param);
        void onCancelled();
        void onPostExecute(Result result);
    }

    private TaskCallbacks<Param, Result> callbacks;
    private Param param;
    private DummyTask task;
    private Result result;

    public static <P, R> void createTask(Activity activity, P param, String tag) {
        TaskFragment<P, R> frag = new TaskFragment<>();
        frag.param = param;
        activity.getFragmentManager().beginTransaction().add(frag, tag).commit();
    }

    public void cancel() {
        task.cancel(true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(!(activity instanceof TaskCallbacks)) {
            throw new IllegalStateException("Activity must implement TaskCallbacks");
        }
        callbacks = (TaskCallbacks)activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        task = new DummyTask();
        task.execute();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
    }

    public Param getParam() {
        return param;
    }

    public Result getResult() {
        return result;
    }

    private class DummyTask extends AsyncTask<Void, Void, Result> {
        @Override
        protected void onPreExecute() {
            if (callbacks != null) {
                callbacks.onPreExecute();
            }
        }

        @Override
        protected Result doInBackground(Void... params) {
            if (callbacks != null) {
                return callbacks.doInBackground(param);
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            if (callbacks != null) {
                callbacks.onCancelled();
            }
            getFragmentManager().beginTransaction().remove(TaskFragment.this).commit();
        }

        @Override
        protected void onPostExecute(Result result) {
            TaskFragment.this.result = result;
            if (callbacks != null) {
                callbacks.onPostExecute(result);
            }
            getFragmentManager().beginTransaction().remove(TaskFragment.this).commit();
        }
    }
}
