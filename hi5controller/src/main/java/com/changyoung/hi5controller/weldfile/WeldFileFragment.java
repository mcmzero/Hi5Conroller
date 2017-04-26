package com.changyoung.hi5controller.weldfile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.EditText;

import com.changyoung.hi5controller.R;
import com.changyoung.hi5controller.common.FileHelper;
import com.changyoung.hi5controller.common.PrefHelper;
import com.changyoung.hi5controller.common.RefreshHandler;
import com.changyoung.hi5controller.common.UiHelper;
import com.changyoung.hi5controller.common.UriHelper;

import java.io.File;

public class WeldFileFragment extends Fragment implements RefreshHandler {
	private static final String TAG = "HI5:WeldFileFragment";
	private static final String ARG_WORK_PATH = "workPath";
	private static final String ARG_WORK_URI = "workUri";
	private static final int OPEN_DIRECTORY_REQUEST_CODE = 1000;

	private EditText mEditTextPath;
	private WeldFileListFragment mWeldFileListFragment;
	private View mView;
	private String mWorkPath;
	private String mWorkUri;
	private FloatingActionButton mFabMain;
	private OnWorkPathListener mListener;

	public WeldFileFragment() {
		// Required empty public constructor
	}

/*
 	public static WeldFileFragment newInstance(String workPath, String workUri) {
		WeldFileFragment fragment = new WeldFileFragment();
		Bundle args = new Bundle();
		args.putString(ARG_WORK_PATH, workPath);
		args.putString(ARG_WORK_URI, workUri);
		fragment.setArguments(args);
		return fragment;
	}
*/

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultDataIntent) {
		logD("onActivityResult");
		switch (requestCode) {
			case OPEN_DIRECTORY_REQUEST_CODE:
				if (resultCode == Activity.RESULT_OK) {
					if (resultDataIntent != null) {
						Uri uri = resultDataIntent.getData();
						if (uri != null) {
							Activity activity = getActivity();

							final int rwFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
							activity.grantUriPermission(activity.getPackageName(), uri, rwFlags);

							final int takeFlags = resultDataIntent.getFlags() & rwFlags;
							activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);

							mWorkPath = UriHelper.getFullPathFromTreeUri(uri, activity);
							mWorkUri = uri.toString();
							onSetWorkUri(mWorkUri, mWorkPath);

							if (mEditTextPath != null) {
								mEditTextPath.setText(mWorkPath);
								logD("etPath:" + mWorkPath);
							}
							show("경로 설정 완료: " + mWorkPath);
						}
					}
				}
				break;
		}
	}

	private void logD(String msg) {
		try {
			Log.i(TAG, msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onRefresh(boolean forced) {
		try {
			mWorkUri = onGetWorkUri();
			mWorkPath = onGetWorkPath();
			logD("[onRefresh]:" + mWorkPath);

			if (mView != null) {
				if (mEditTextPath != null)
					mEditTextPath.setText(mWorkPath);
			}

			if (isAdded()) {
				WeldFileListFragment workPathFragment = (WeldFileListFragment) getChildFragmentManager().findFragmentById(R.id.weldfile_fragment);
				if (workPathFragment != null)
					workPathFragment.refreshFilesList(mWorkPath);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean onRefresh(String path) {
		if (path != null && isAdded()) {
			try {
				File dir = new File(path);
				if (dir.isDirectory()) {
					if (!isDetached()) {
						WeldFileListFragment workPathFragment = (WeldFileListFragment) getChildFragmentManager().findFragmentById(R.id.weldfile_fragment);
						if (workPathFragment != null)
							workPathFragment.refreshFilesList(dir);
					}
				}
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				logD("onRefresh");
			}
		} else {
			logD("isAdded() == false, onRefresh:" + path);
		}
		return false;
	}

	public String onRefresh(int menuId) {
		String ret = null;
		switch (menuId) {
			case R.id.nav_home:
				if (!onRefresh(onGetWorkPath()))
					ret = "경로 이동 실패: " + onGetWorkPath();
				break;
			case R.id.nav_storage:
				if (!onRefresh(PrefHelper.STORAGE_PATH))
					ret = "경로 이동 실패: " + PrefHelper.STORAGE_PATH;
				break;
			case R.id.nav_sdcard:
				if (!onRefresh(PrefHelper.EXTERNAL_STORAGE_PATH))
					ret = "경로 이동 실패: " + PrefHelper.EXTERNAL_STORAGE_PATH;
				break;
			case R.id.nav_extsdcard:
				ret = "경로 이동 실패: " + "SD 카드";
				try {
					File dir = new File(PrefHelper.STORAGE_PATH);
					File[] dirs = dir.listFiles();
					if (dirs != null) {
						for (File file : dirs) {
							if (file.getName().toLowerCase().startsWith("ext") || file.getName().toLowerCase().startsWith("sdcard1")) {
								try {
									for (File subItem : file.listFiles()) {
										if (subItem.exists() && onRefresh(file.getPath())) {
											ret = null;
											break;
										}
									}
								} catch (NullPointerException e) {
									Log.i(TAG, e.getLocalizedMessage());
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					logD("경로 이동 실패");
				}
				break;
			case R.id.nav_usbstorage:
				ret = "경로 이동 실패: " + "USB 저장소";
				try {
					File dir = new File(PrefHelper.STORAGE_PATH);
					Log.i(TAG, String.format("STORAGE: %s", dir.getPath().toLowerCase()));
					File[] dirs = dir.listFiles();
					if (dirs != null) {
						for (File file : dirs) {
							if (file.getName().toLowerCase().startsWith("usb")) {
								try {
									File[] filteredFiles = file.listFiles();
									if (filteredFiles != null) {
										for (File subItem : filteredFiles) {
											if (subItem.exists() && onRefresh(file.getPath())) {
												Log.i(TAG, String.format("USB: %s", file.getPath().toLowerCase()));
												ret = null;
												break;
											}
										}
									}
								} catch (NullPointerException e) {
									Log.i(TAG, e.getLocalizedMessage());
								}
							}
						}
						if (ret != null) {
							for (File file : dirs) {
								String filename = file.getName();
								if (!filename.equalsIgnoreCase("emulated")
										&& !filename.equalsIgnoreCase("enc_emulated")
										&& !filename.equalsIgnoreCase("self")) {
									File[] filteredFiles = file.listFiles();
									if (filteredFiles != null) {
										for (File subItem : filteredFiles) {
											if (subItem.exists() && onRefresh(file.getPath())) {
												Log.i(TAG, String.format("USB: %s", file.getPath().toLowerCase()));
												ret = null;
												break;
											}
										}
									}
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					logD("경로 이동 실패");
				}
				break;
		}
		return ret;
	}

	@Override
	public String onBackPressedFragment() {
		return isAdded() ? mWeldFileListFragment.refreshParent() : null;
	}

	@Override
	public void show(String msg) {
		try {
			if (msg == null)
				return;
			if (mView != null) {
				Snackbar.make(mView, msg, Snackbar.LENGTH_LONG)
						.setAction("Action", null).show();
			}
			logD(msg);
		} catch (Exception e) {
			e.printStackTrace();
			logD(msg);
		}
	}

	public void onPathChanged(String path) {
		try {
			if (isAdded()) {    // Return true if the mWeldFileListFragment is currently added to its activity.
				mWorkPath = onGetWorkPath();
				if (mEditTextPath != null)
					mEditTextPath.setText(path);
//				FloatingActionButton fab = (FloatingActionButton) mView.findViewById(R.id.fab_work_path_main);
//				if (fab != null) {
//					if (mWorkPath.compareTo(path) == 0) {
//						fab.setImageResource(R.drawable.ic_archive_white);
//					} else {
//						fab.setImageResource(R.drawable.ic_done_white);
//					}
//				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

//	public View getFab() {
//		return mFabMain;
//	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			mWorkPath = getArguments().getString(ARG_WORK_PATH);
			mWorkPath = getArguments().getString(ARG_WORK_URI);
		} else {
			mWorkPath = onGetWorkPath();
			mWorkUri = onGetWorkUri();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView");
		mView = inflater.inflate(R.layout.weldfile_fragment, container, false);

		String path = mWorkPath;
		mWeldFileListFragment = (WeldFileListFragment) getChildFragmentManager().findFragmentById(R.id.weldfile_fragment);
		if (mWeldFileListFragment == null) {
			Log.i(TAG, "mWeldFileListFragment == null");
			mWeldFileListFragment = WeldFileListFragment.newInstance(path);
			FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
			transaction.replace(R.id.weldfile_fragment, mWeldFileListFragment);
			transaction.addToBackStack(null);
			transaction.commit();
		}

		mWeldFileListFragment.snackbarView = mView;
		mWeldFileListFragment.refreshFilesList(path);
		mEditTextPath = (EditText) mView.findViewById(R.id.etWorkPath);
		mEditTextPath.setText(path);
		mEditTextPath.setOnFocusChangeListener((v, hasFocus) -> {
			logD("FabStorage");
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				if (hasFocus) {
					v.clearFocus();
					Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
					int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
					flags |= Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
					flags |= Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
					intent.setFlags(flags);
					startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE);
					logD("FabStorage:OPEN_DIRECTORY_REQUEST_CODE");
				}
			} else {
				WeldFileListFragment fragment = (WeldFileListFragment) getChildFragmentManager().findFragmentById(R.id.weldfile_fragment);
				@SuppressLint("CutPasteId") EditText etPath1 = (EditText) mView.findViewById(R.id.etWorkPath);
				try {
					File file = new File(etPath1.getText().toString());
					if (file.isDirectory()) {
						onSetWorkPath(etPath1.getText().toString());
					} else {
						throw new Exception();
					}
				} catch (NullPointerException e) {
					logD(e.getLocalizedMessage());
				} catch (Exception e) {
					e.printStackTrace();
					show("잘못된 경로: " + etPath1.getText().toString());
					etPath1.setText(onGetWorkPath());
				}
				fragment.refreshFilesList(etPath1.getText().toString());
			}
		});
		mEditTextPath.setOnKeyListener((v, keyCode, event) -> {
			UiHelper.hideSoftKeyboard(getActivity(), v, event);
			return false;
		});

		FloatingActionButton mFabHome = (FloatingActionButton) mView.findViewById(R.id.fab_workpath_home);
		if (mFabHome != null) {
			mFabHome.setOnClickListener(v -> {
				show(onRefresh(R.id.nav_home));
				logD("FabHome");
			});
		}

		FloatingActionButton mFabStorage = (FloatingActionButton) mView.findViewById(R.id.fab_workpath_storage);
		if (mFabStorage != null) {
			mFabStorage.setOnClickListener(v -> {
				logD("FabStorage");
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
					int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
					flags |= Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
					flags |= Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
					intent.setFlags(flags);
					startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE);
					logD("FabStorage:OPEN_DIRECTORY_REQUEST_CODE");
				} else {
					show(onRefresh(R.id.nav_usbstorage));
				}
			});
		}

		mFabMain = (FloatingActionButton) mView.findViewById(R.id.fab_workpath_main);
		if (mFabMain != null) {
			//noinspection SameParameterValue,SameParameterValue,SameParameterValue,SameParameterValue,SameParameterValue,SameParameterValue
			mFabMain.setOnClickListener(new View.OnClickListener() {
				private void scaleAnimationFab(@SuppressWarnings("SameParameterValue") final float from, @SuppressWarnings("SameParameterValue") final float to) {
					ScaleAnimation shrink = new ScaleAnimation(from, to, from, to,
							Animation.RELATIVE_TO_SELF, 0.5f,
							Animation.RELATIVE_TO_SELF, 0.5f);
					shrink.setDuration(250);
					shrink.setInterpolator(new AccelerateInterpolator());
					shrink.setAnimationListener(new Animation.AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {

						}

						@Override
						public void onAnimationEnd(Animation animation) {
							ScaleAnimation expand = new ScaleAnimation(to, from, to, from,
									Animation.RELATIVE_TO_SELF, 0.5f,
									Animation.RELATIVE_TO_SELF, 0.5f);
							expand.setDuration(250);
							expand.setInterpolator(new DecelerateInterpolator());
							mFabMain.startAnimation(expand);
						}

						@Override
						public void onAnimationRepeat(Animation animation) {

						}
					});
					mFabMain.startAnimation(shrink);
				}

				@Override
				public void onClick(View v) {
					scaleAnimationFab(1.0f, 1.5f);
					UiHelper.hideSoftKeyboard(getActivity(), null, null);
					WeldFileListFragment fragment = (WeldFileListFragment) getChildFragmentManager()
							.findFragmentById(R.id.weldfile_fragment);
					String path = fragment.getDirPath();
					mWorkPath = onGetWorkPath();
					if (mWorkPath != null) {
						if (mWorkPath.compareTo(path) == 0) {
//						ActivityOptions options = ActivityOptions
//								.makeSceneTransitionAnimation(getActivity(), fab, "fab");
//						startActivity(new Intent(getContext(), WeldRestoreActivity.class), options.toBundle());
							String ret = FileHelper.backupDocumentFile(getContext(), mView);
							if (ret != null)
								show(ret);
						} else {
							if (mEditTextPath != null)
								mEditTextPath.setText(path);
							onSetWorkPath(path);
							fragment.refreshFilesList();
							show("경로 설정 완료: " + path);
						}
					}
				}
			});
		}

/*
		android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) mView.findViewById(R.id.work_path_toolbar);
		toolbar.inflateMenu(R.menu.menu_toolbar_work_path);
		toolbar.setOnMenuItemClickListener(item -> {
			String ret = onRefresh(item.getItemId());
			if (ret != null)
				show(ret);
			return true;
		});
*/

/*
		// 배너 광고
		AdView adView = new AdView(getContext());
		adView.setAdSize(AdSize.BANNER);
		adView.setScaleX(0.4f);
		adView.setScaleY(0.4f);
		adView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
				FrameLayout.LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM | Gravity.START));
		if (BuildConfig.DEBUG)
			adView.setAdUnitId(getActivity().getString(R.string.banner_ad_unit_id_debug));
		else
			adView.setAdUnitId(getActivity().getString(R.string.banner_ad_unit_id_release));
		AdRequest adRequest = new AdRequest.Builder()
				.setRequestAgent("android_studio:ad_template").build();
		adView.loadAd(adRequest);
		FrameLayout frameLayout = (FrameLayout) mView.findViewById(R.id.frame_layout);
		frameLayout.addView(adView, frameLayout.getChildCount() - 1);
*/

/*
		NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
		if (navigationView != null) {
			navigationView.setNavigationItemSelectedListener(WeldFileFragment.this);
		}
*/

		return mView;
	}

	private void onSetWorkPath(String path) {
		if (mListener != null) {
			mListener.onSetWorkPath(path);
		}
	}

	private String onGetWorkPath() {
		if (mListener != null) {
			return mListener.onGetWorkPath();
		}
		return null;
	}

	private void onSetWorkUri(String uri, String path) {
		if (mListener != null) {
			mListener.onSetWorkUri(uri, path);
		}
	}

	private String onGetWorkUri() {
		if (mListener != null) {
			return mListener.onGetWorkUri();
		}
		return null;
	}

	@Override
	public void onAttach(Context activity) {
		super.onAttach(activity);
		try {
			mListener = (OnWorkPathListener) activity;
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new ClassCastException(activity.toString() + " must implement OnPathChangedListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
		mView = null;
		mWorkPath = null;
		mWeldFileListFragment = null;
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * mWeldFileListFragment to allow an interaction in this mWeldFileListFragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p/>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnWorkPathListener {
		String onGetWorkPath();
		void onSetWorkPath(String path);
		String onGetWorkUri();
		void onSetWorkUri(String uri, String path);
	}
}